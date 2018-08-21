package org.radarbase.connect.rest.fitbit.route;

import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.TIMESTAMP_OFFSET_KEY;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.ws.rs.NotAuthorizedException;
import okhttp3.Request;
import okhttp3.Response;
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
  protected static final Duration TOO_MANY_REQUESTS_COOLDOWN = Duration.ofHours(1);
  protected static final Duration ONE_DAY = Duration.ofDays(1);

  private static final Logger logger = LoggerFactory.getLogger(FitbitSleepRoute.class);

  /** Committed committedOffsets. */
  private final Map<String, Long> committedOffsets;

  /** Offsets that are scanned but are currently empty. */
  private final Map<String, Long> scannedOffsets;
  private final Map<String, Map<String, Object>> partitions;
  private final FitbitRequestGenerator generator;
  private final FitbitUserRepository userRepository;
  private final String routeName;
  private Instant tooManyRequestsUntil;
  private long pollInterval;
  private long lastPoll;
  private String baseUrl;

  public FitbitPollingRoute(FitbitRequestGenerator generator, FitbitUserRepository userRepository, String routeName) {
    this.generator = generator;
    this.userRepository = userRepository;
    this.committedOffsets = new HashMap<>();
    this.scannedOffsets = new HashMap<>();
    this.partitions = new HashMap<>(generator.getPartitions(routeName));
    this.routeName = routeName;
    this.lastPoll = 0L;
    this.tooManyRequestsUntil = Instant.MIN;
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    this.pollInterval = config.getPollInterval();
    this.baseUrl = config.getUrl();
    this.converter().initialize(config);
  }

  @Override
  public void requestSucceeded(RestRequest request, SourceRecord record) {
    String userKey = ((FitbitRestRequest) request).getUser().getId();
    long offset = (Long) record.sourceOffset().get(TIMESTAMP_OFFSET_KEY);
    committedOffsets.put(userKey, offset);
    scannedOffsets.put(userKey, offset);
  }

  @Override
  public void requestEmpty(RestRequest request) {
    FitbitRestRequest fitbitRequest = (FitbitRestRequest) request;
    Instant endOffset = fitbitRequest.getEndOffset();
    String key = fitbitRequest.getUser().getId();
    scannedOffsets.put(key, endOffset.toEpochMilli());

    if (passedInterval(endOffset, HISTORICAL_TIME)) {
      committedOffsets.put(key, endOffset.toEpochMilli());
    }
  }

  @Override
  public void requestFailed(RestRequest request, Response response) {
    if (response != null && response.code() == 429) {
      logger.info("Too many requests for user {}. Backing off for {}",
          ((FitbitRestRequest)request).getUser(), TOO_MANY_REQUESTS_COOLDOWN);
      tooManyRequestsUntil = Instant.now().plus(TOO_MANY_REQUESTS_COOLDOWN);
    } else {
      logger.warn("Failed to make request {}", request);
    }
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

  protected FitbitRestRequest newRequest(FitbitUser user,
      Instant startDate, Instant endDate, Object... urlFormatArgs) {
    Request.Builder builder = new Request.Builder()
        .url(String.format(getUrlFormat(baseUrl), urlFormatArgs));
    try {
      Request request = builder
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
            e -> (Long) e.getValue().get(TIMESTAMP_OFFSET_KEY))));

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
    Instant scannedOffset = Instant.ofEpochMilli(
        scannedOffsets.computeIfAbsent(key, k -> getStartOffset(user).toEpochMilli()));

    if (passedInterval(scannedOffset, getLookbackTime())) {
      Instant committedOffset = Instant.ofEpochMilli(
          committedOffsets.getOrDefault(key, getStartOffset(user).toEpochMilli()));
      if (!passedInterval(committedOffset, getLookbackTime())) {
        scannedOffset = committedOffset;
      }
    }

    return scannedOffset;
  }

  protected abstract String getUrlFormat(String baseUrl);

  private static Instant getStartOffset(FitbitUser user) {
    return user.getStartDate().minus(Duration.ofSeconds(1));
  }

  private static boolean passedInterval(Instant pastInstant, Duration duration) {
    return pastInstant.plus(duration).isAfter(Instant.now());
  }

  protected Duration getLookbackTime() {
    return LOOKBACK_TIME;
  }

  protected Instant nextPoll(FitbitUser user) {
    Instant offset = getOffset(user);
    if (offset.isAfter(user.getEndDate())) {
      return Instant.MAX;
    } else {
      return max(offset.plus(getLookbackTime()), tooManyRequestsUntil);
    }
  }

  public static <T extends Comparable<? super T>> T max(T a, T b) {
    if (a != null && (b == null || a.compareTo(b) >= 0)) {
      return a;
    } else {
      return b;
    }
  }
}
