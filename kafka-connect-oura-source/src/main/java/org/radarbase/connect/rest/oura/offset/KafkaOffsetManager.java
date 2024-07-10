package org.radarbase.connect.rest.oura.offset;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Instant;
import java.util.List;
import java.time.Duration;
import static java.time.temporal.ChronoUnit.NANOS;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.radarbase.oura.offset.Offset;
import org.radarbase.oura.route.Route;
import org.radarbase.oura.user.User;
import org.radarbase.oura.request.OuraOffsetManager;

public class KafkaOffsetManager implements OuraOffsetManager {
  private OffsetStorageReader offsetStorageReader;
  private Map<String, Instant> offsets;
  private static final Logger logger = LoggerFactory.getLogger(KafkaOffsetManager.class);

  String TIMESTAMP_OFFSET_KEY = "timestamp";
  protected static final Duration ONE_NANO = NANOS.getDuration();

  public KafkaOffsetManager(
    OffsetStorageReader offsetStorageReader) {
      this.offsetStorageReader = offsetStorageReader;
    }

  public void initialize(List<Map<String, Object>> partitions) {
    if (this.offsetStorageReader != null) {
      this.offsets = this.offsetStorageReader.offsets(partitions).entrySet().stream()
      .filter(e -> e.getValue() != null && e.getValue().containsKey(TIMESTAMP_OFFSET_KEY))
      .collect(Collectors.toMap(
          e -> (String) e.getKey().get("user") + "-" + e.getKey().get("route"),
          e -> Instant.ofEpochSecond(((Number) e.getValue().get(TIMESTAMP_OFFSET_KEY)).longValue())));
    } else {
      logger.warn("Offset storage reader is null, will resume from an empty state.");
    }
  }

  @Override
  public Offset getOffset(Route route, User user) {
    Instant offset = offsets.getOrDefault(getOffsetKey(route, user), user.getStartDate().minus(ONE_NANO));
    return new Offset(user, route, offset);
  }

  @Override
  public void updateOffsets(Route route, User user, Instant offset) {
    offsets.put(getOffsetKey(route, user), offset);
  }

  private String getOffsetKey(Route route, User user) {
    return user.getVersionedId() + "-" + route.toString();
  }
}

