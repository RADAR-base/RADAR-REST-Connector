package org.radarbase.connect.rest.fitbit.route;

import okhttp3.Request;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.FitbitUser;
import org.radarbase.connect.rest.fitbit.FitbitUserRepository;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.request.PollingRequestRoute;
import org.radarbase.connect.rest.request.RestProcessedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.TIMESTAMP_OFFSET_KEY;

public abstract class FitbitPollingRoute implements PollingRequestRoute {
  protected static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  protected static final long LOOKBACK_TIME = 86400; // 1 day

  private static final Logger logger = LoggerFactory.getLogger(FitbitSleepRoute.class);

  private final Map<String, Long> offsets;
  private final Map<String, Map<String, Object>> partitions;
  private final FitbitRequestGenerator generator;
  private final FitbitUserRepository userRepository;
  private final String routeName;
  private Long pollInterval;

  public FitbitPollingRoute(FitbitRequestGenerator generator, FitbitUserRepository userRepository, String routeName) {
    this.generator = generator;
    this.userRepository = userRepository;
    this.offsets = new HashMap<>();
    this.partitions = new HashMap<>(generator.getPartitions(routeName));
    this.routeName = routeName;
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    this.pollInterval = config.getPollInterval();
  }


  @Override
  public void requestSucceeded(RestProcessedResponse processedResponse) {
    FitbitUser user = ((FitbitRestRequest) processedResponse.getRequest()).getUser();
    offsets.put(user.getKey(), (Long)processedResponse.getRecord().sourceOffset()
        .get(TIMESTAMP_OFFSET_KEY));
  }

  /**
   * Actually construct a request, based on the current offset
   * @param user Fitbit user
   * @return request to make
   */
  protected abstract Request makeRequest(FitbitUser user);

  @Override
  public Stream<FitbitRestRequest> requests() {
    try {
      return userRepository.stream()
          .filter(u -> nextPoll(u) >= System.currentTimeMillis())
          .map(u -> new FitbitRestRequest(this, makeRequest(u), u, getPartition(u),
              generator.getClient(u)));
    } catch (IOException e) {
      logger.warn("Cannot read users");
      return Stream.empty();
    }
  }

  private Map<String, Object> getPartition(FitbitUser user) {
    return partitions.computeIfAbsent(user.getKey(),
        k -> generator.getPartition(routeName, user));
  }

  @Override
  public void setOffsetStorageReader(OffsetStorageReader offsetStorageReader) {
    offsets.putAll(offsetStorageReader.offsets(partitions.values()).entrySet().stream()
        .collect(Collectors.toMap(
            e -> (String) e.getKey().get("user"),
            e -> (Long) e.getValue().get(TIMESTAMP_OFFSET_KEY))));
  }

  @Override
  public long getPollInterval() {
    return pollInterval;
  }

  @Override
  public LongStream nextPolls() {
    try {
      return userRepository.stream()
          .mapToLong(this::nextPoll);
    } catch (IOException e) {
      logger.warn("Failed to read users for polling interval: {}", e.toString());
      return LongStream.of(getPollInterval());
    }
  }

  protected long getOffset(FitbitUser user) {
    return offsets.getOrDefault(user.getKey(), 0L);
  }

  protected long nextPoll(FitbitUser user) {
    return getOffset(user) + LOOKBACK_TIME + pollInterval;
  }
}
