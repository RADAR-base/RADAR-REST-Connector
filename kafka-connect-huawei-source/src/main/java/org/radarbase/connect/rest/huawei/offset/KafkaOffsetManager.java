/*
 * Copyright 2026 Onsentia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.radarbase.connect.rest.huawei.offset;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static java.time.temporal.ChronoUnit.NANOS;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.huawei.offset.Offset;
import org.radarbase.huawei.request.HuaweiOffsetManager;
import org.radarbase.huawei.route.Route;
import org.radarbase.huawei.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaOffsetManager implements HuaweiOffsetManager {
  private static final Logger logger = LoggerFactory.getLogger(KafkaOffsetManager.class);
  private static final String TIMESTAMP_OFFSET_KEY = "timestamp";
  private static final Duration ONE_NANO = NANOS.getDuration();

  private final OffsetStorageReader offsetStorageReader;
  private Map<String, Instant> offsets;

  public KafkaOffsetManager(OffsetStorageReader offsetStorageReader) {
    this.offsetStorageReader = offsetStorageReader;
  }

  public void initialize(List<Map<String, Object>> partitions) {
    if (this.offsetStorageReader != null) {
      this.offsets = this.offsetStorageReader.offsets(partitions).entrySet().stream()
          .filter(e -> e.getValue() != null && e.getValue().containsKey(TIMESTAMP_OFFSET_KEY))
          .collect(Collectors.toMap(
              e -> e.getKey().get("user") + "-" + e.getKey().get("route"),
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
