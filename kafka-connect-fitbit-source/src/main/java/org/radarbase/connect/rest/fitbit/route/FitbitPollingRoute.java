package org.radarbase.connect.rest.fitbit.route;

import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.TIMESTAMP_OFFSET_KEY;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.ws.rs.NotAuthorizedException;
import okhttp3.Request;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;
import org.radarbase.connect.rest.request.PollingRequestRoute;
import org.radarbase.connect.rest.request.RestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FitbitPollingRoute implements PollingRequestRoute {
  protected static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  protected static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
  protected static final Duration LOOKBACK_TIME = Duration.ofDays(1); // 1 day
  protected static final Duration HISTORICAL_TIME = Duration.ofDays(14);

  private static final Logger logger = LoggerFactory.getLogger(FitbitSleepRoute.class);

  /** Committed committedOffsets. */
  private final Map<String, Instant> committedOffsets;

  /** Offsets that are scanned but are currently empty. */
  private final Map<String, Instant> scannedOffsets;
  private final Map<String, Map<String, Object>> partitions;
  private final FitbitRequestGenerator generator;
  private final FitbitUserRepository userRepository;
  private final String routeName;
  private long pollInterval;
  private long lastPoll;

  public FitbitPollingRoute(FitbitRequestGenerator generator, FitbitUserRepository userRepository, String routeName) {
    this.generator = generator;
    this.userRepository = userRepository;
    this.committedOffsets = new HashMap<>();
    this.scannedOffsets = new HashMap<>();
    this.partitions = new HashMap<>(generator.getPartitions(routeName));
    this.routeName = routeName;
    this.lastPoll = 0L;
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    this.pollInterval = config.getPollInterval();
  }

  @Override
  public void requestSucceeded(RestRequest request, SourceRecord record) {
    String userKey = ((FitbitRestRequest) request).getUser().getId();
    Instant offset = (Instant) record.sourceOffset().get(TIMESTAMP_OFFSET_KEY);
    committedOffsets.put(userKey, offset);
    scannedOffsets.put(userKey, offset);
  }

  @Override
  public void requestEmpty(RestRequest request) {
    FitbitRestRequest fitbitRequest = (FitbitRestRequest) request;
    Instant endOffset = fitbitRequest.getEndOffset();
    String key = fitbitRequest.getUser().getId();
    scannedOffsets.put(key, endOffset);

    if (passedInterval(endOffset, HISTORICAL_TIME)) {
      committedOffsets.put(key, endOffset);
    }
  }

  @Override
  public void requestFailed(RestRequest request) {
    logger.warn("Failed to make request {}", request);
  }

  /**
   * Actually construct a request, based on the current offset
   * @param user Fitbit user
   * @return request to make
   */
  protected abstract FitbitRestRequest makeRequest(FitbitUser user);

  @Override
  public Stream<FitbitRestRequest> requests() {
    lastPoll = System.currentTimeMillis();
    try {
      return userRepository.stream()
          .filter(u -> !nextPoll(u).isAfter(Instant.now()))
          .map(this::makeRequest)
          .filter(Objects::nonNull);
    } catch (IOException e) {
      logger.warn("Cannot read users");
      return Stream.empty();
    }
  }

  private Map<String, Object> getPartition(FitbitUser user) {
    return partitions.computeIfAbsent(user.getId(),
        k -> generator.getPartition(routeName, user));
  }

  protected FitbitRestRequest newRequest(Request.Builder requestBuilder, FitbitUser user,
      Instant startDate, Instant endDate) {
    try {
      Request request = requestBuilder
          .header("Authorization", "Bearer " + userRepository.getAccessToken(user))
          .build();
      return new FitbitRestRequest(this, request, user, getPartition(user),
          generator.getClient(user), startDate, endDate);
    } catch (NotAuthorizedException | IOException ex) {
      logger.warn("User {} does not have a configured access token: {}. Skipping.",
          user, ex.toString());
      return null;
    }
  }

  @Override
  public void setOffsetStorageReader(OffsetStorageReader offsetStorageReader) {
    committedOffsets.putAll(offsetStorageReader.offsets(partitions.values()).entrySet().stream()
        .filter(e -> e.getValue() != null && e.getValue().containsKey(TIMESTAMP_OFFSET_KEY))
        .collect(Collectors.toMap(
            e -> (String) e.getKey().get("user"),
            e -> (Instant) e.getValue().get(TIMESTAMP_OFFSET_KEY))));

    scannedOffsets.putAll(committedOffsets);
  }

  @Override
  public long getPollInterval() {
    return pollInterval;
  }

  @Override
  public LongStream nextPolls() {
    try {
      return userRepository.stream()
          .map(this::nextPoll)
          .mapToLong(Instant::toEpochMilli);
    } catch (IOException e) {
      logger.warn("Failed to read users for polling interval: {}", e.toString());
      return LongStream.of(getPollInterval());
    }
  }

  public long getLastPoll() {
    return lastPoll;
  }

  protected Instant getOffset(FitbitUser user) {
    String key = user.getId();
    Instant scannedOffset = scannedOffsets.computeIfAbsent(key, k -> getStartOffset(user));

    if (passedInterval(scannedOffset, LOOKBACK_TIME)) {
      Instant committedOffset = committedOffsets.getOrDefault(key, getStartOffset(user));
      if (!passedInterval(committedOffset, LOOKBACK_TIME)) {
        scannedOffset = committedOffset;
      }
    }

    return scannedOffset;
  }

  private static Instant getStartOffset(FitbitUser user) {
    return user.getStartDate().minus(Duration.ofSeconds(1));
  }

  private static boolean passedInterval(Instant pastInstant, Duration duration) {
    return pastInstant.plus(duration).isAfter(Instant.now());
  }

  protected Instant nextPoll(FitbitUser user) {
    Instant offset = getOffset(user);
    if (offset.isAfter(user.getEndDate())) {
      return Instant.MAX;
    } else {
      return offset.plus(LOOKBACK_TIME);
    }
  }
}
