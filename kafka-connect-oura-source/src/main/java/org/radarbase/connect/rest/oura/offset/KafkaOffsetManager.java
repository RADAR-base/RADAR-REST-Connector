package org.radarbase.connect.rest.oura.offset;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Instant;
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
  private Map<String, Instant> offsets;
  private static final Logger logger = LoggerFactory.getLogger(KafkaOffsetManager.class);

  String TIMESTAMP_OFFSET_KEY = "timestamp";
  protected static final Duration ONE_NANO = NANOS.getDuration();

  public KafkaOffsetManager(
    OffsetStorageReader offsetStorageReader,
    Map<String, Map<String, Object>> partitions) {
    this.offsets = offsetStorageReader.offsets(partitions.values()).entrySet().stream()
      .filter(e -> e.getValue() != null && e.getValue().containsKey(TIMESTAMP_OFFSET_KEY))
      .collect(Collectors.toMap(
          e -> (String) e.getKey().get("user"),
          e -> Instant.ofEpochMilli((Long) e.getValue().get(TIMESTAMP_OFFSET_KEY))));
  }

  @Override
  public Offset getOffset(Route route, User user) {
    Instant offset = offsets.getOrDefault(user.getVersionedId(), user.getStartDate().minus(ONE_NANO));
    return new Offset(user.getUserId(), route.toString(), offset);
  }

  @Override
  public void updateOffsets(Route route, User user, Instant offset) {
    offsets.put(user.getVersionedId(), offset);
  }
}

