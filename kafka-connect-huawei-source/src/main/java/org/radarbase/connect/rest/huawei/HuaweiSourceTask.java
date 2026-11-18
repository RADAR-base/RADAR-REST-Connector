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

package org.radarbase.connect.rest.huawei;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.huawei.offset.KafkaOffsetManager;
import org.radarbase.connect.rest.huawei.user.HuaweiUserRepository;
import org.radarbase.connect.rest.huawei.util.VersionUtil;
import org.radarbase.huawei.converter.TopicData;
import org.radarbase.huawei.request.HuaweiRequestGenerator;
import org.radarbase.huawei.request.HuaweiResult;
import org.radarbase.huawei.request.HuaweiResult.Success;
import org.radarbase.huawei.request.HuaweiResult.Error;
import org.radarbase.huawei.request.HuaweiErrorBase;
import org.radarbase.huawei.request.RestRequest;
import org.radarbase.huawei.route.HuaweiRouteDefinition;
import org.radarbase.huawei.route.HuaweiRouteFactory;
import org.radarbase.huawei.route.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.radarbase.huawei.user.User;
import io.confluent.connect.avro.AvroData;
import kotlin.streams.jdk8.StreamsKt;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * @author yatharthranjan
 */
public class HuaweiSourceTask extends SourceTask {
  private static final Logger logger = LoggerFactory.getLogger(HuaweiSourceTask.class);

  private OkHttpClient baseClient;
  private HuaweiUserRepository userRepository;
  private List<Route> routes;
  private HuaweiRequestGenerator huaweiRequestGenerator;
  private final AvroData avroData = new AvroData(20);
  private static final AvroData HEARTBEAT_AVRO_DATA = new AvroData(1);
  private KafkaOffsetManager offsetManager;
  private String heartbeatTopic;
  private static final String TIMESTAMP_OFFSET_KEY = "timestamp";
  private static final long TIMEOUT = 60000L;
  private int routeStartIndex = 0;
  private final CountDownLatch stopLatch = new CountDownLatch(1);
  private boolean lastPollHadRecords = false;

  public void initialize(
      HuaweiRestSourceConnectorConfig config, OffsetStorageReader offsetStorageReader) {
    this.baseClient = new OkHttpClient();

    this.userRepository = config.getUserRepository();
    this.offsetManager = new KafkaOffsetManager(offsetStorageReader);
    this.routes = getRoutes(config);
    this.heartbeatTopic = config.getHeartbeatTopic();
    this.huaweiRequestGenerator = new HuaweiRequestGenerator(
        this.userRepository,
        this.offsetManager,
        this.routes,
        config.getLateSyncWindow(),
        config.getMaxHistory(),
        config.getPollIntervalPerUser());
    this.offsetManager.initialize(getPartitions());
  }

  private List<Route> getRoutes(HuaweiRestSourceConnectorConfig config) {
    Map<String, String> enabledTopics = config.enabledTopics();
    List<Route> result = new ArrayList<>();
    for (HuaweiRouteDefinition definition : HuaweiRouteFactory.INSTANCE.getDefinitions()) {
      String topic = enabledTopics.get(definition.getKey());
      if (topic != null) {
        result.add(definition.getBuild().invoke(userRepository, topic));
      }
    }
    return result;
  }

  public List<Map<String, Object>> getPartitions() {
    try {
      return StreamsKt.asStream(userRepository.stream())
          .flatMap(u -> this.routes.stream().map(r -> getPartition(r.toString(), u)))
          .collect(Collectors.toList());
    } catch (Exception e) {
      logger.warn("Failed to initialize user partitions..");
      return Collections.emptyList();
    }
  }

  public Map<String, Object> getPartition(String route, User user) {
    Map<String, Object> partition = new HashMap<>(4);
    partition.put("user", user.getVersionedId());
    partition.put("route", route);
    return partition;
  }

  public Stream<RestRequest> requests() {
    if (this.routes == null || this.routes.isEmpty()) {
      return Stream.empty();
    }

    // Rotate routes so that all routes are requested in a round-robin manner
    List<Route> rotatedRoutes = getRotatedRoutes();
    return rotatedRoutes.stream()
        .flatMap((Route r) -> StreamsKt.asStream(huaweiRequestGenerator.requests(r, 100)));
  }

  private List<Route> getRotatedRoutes() {
    List<Route> rotatedRoutes = new ArrayList<>(this.routes);
    Collections.rotate(rotatedRoutes, routeStartIndex % this.routes.size());
    routeStartIndex = (routeStartIndex + 1) % this.routes.size();
    return rotatedRoutes;
  }

  public Stream<SourceRecord> handleRequest(RestRequest req) throws IOException {
    try (Response response = baseClient.newCall(req.getRequest()).execute()) {
      Instant before = currentOffset(req);
      HuaweiResult<List<TopicData>> result =
          this.huaweiRequestGenerator.handleResponse(req, response);
      if (result instanceof HuaweiResult.Success) {
        Success<List<TopicData>> success = (Success<List<TopicData>>) result;
        List<TopicData> records = success.getValue();
        Map<String, Object> partition = getPartition(req.getRoute().toString(), req.getUser());
        List<SourceRecord> sourceRecords = records.stream()
            .map(r -> sourceRecord(partition, r))
            .collect(Collectors.toList());
        SourceRecord heartbeat = heartbeat(heartbeatTopic, partition,
            req.getUser().getObservationKey(), before, currentOffset(req), records);
        if (heartbeat != null) {
          sourceRecords.add(heartbeat);
        }
        return sourceRecords.stream();
      } else {
        HuaweiErrorBase e = (HuaweiErrorBase) ((HuaweiResult.Error) result).getError();
        logger.warn("Failed to make request: {} {} {}", e.getMessage(), e.getCause(), e.getCode());
        return Stream.empty();
      }
    }
  }

  private Instant currentOffset(RestRequest req) {
    return offsetManager.getOffset(req.getRoute(), req.getUser()).getOffset();
  }

  private SourceRecord sourceRecord(Map<String, Object> partition, TopicData r) {
    SchemaAndValue avro = avroData.toConnectData(r.getValue().getSchema(), r.getValue());
    SchemaAndValue key = avroData.toConnectData(r.getKey().getSchema(), r.getKey());
    // Stored offsets mark where to resume: just after this record's start time (seconds).
    Map<String, ?> offset = Collections.singletonMap(TIMESTAMP_OFFSET_KEY, r.getOffset() + 1);

    return new SourceRecord(partition, offset, r.getTopic(),
        key.schema(), key.value(), avro.schema(), avro.value());
  }

  /**
   * Kafka Connect only stores an offset along with a record. When a request moved the offset
   * past what its records store, which happens once a time window without data has settled,
   * an empty record on the heartbeat topic stores it, so a restart does not request that
   * window again.
   *
   * @return the heartbeat record, or null if the records already store the offset.
   */
  static SourceRecord heartbeat(String topic, Map<String, Object> partition,
      SpecificRecord key, Instant before, Instant after, List<TopicData> records) {
    long stored = records.stream()
        .mapToLong(r -> r.getOffset() + 1)
        .max()
        .orElse(before.getEpochSecond());
    if (!after.isAfter(before) || after.getEpochSecond() <= stored) {
      return null;
    }
    Map<String, ?> offset = Collections.singletonMap(TIMESTAMP_OFFSET_KEY, after.getEpochSecond());
    SchemaAndValue connectKey = HEARTBEAT_AVRO_DATA.toConnectData(key.getSchema(), key);
    return new SourceRecord(partition, offset, topic,
        connectKey.schema(), connectKey.value(), null, null);
  }

  @Override
  public void start(Map<String, String> map) {
    HuaweiRestSourceConnectorConfig connectorConfig;
    try {
      Class<?> connector = Class.forName(map.get("connector.class"));
      Object connectorInst = connector.getConstructor().newInstance();
      connectorConfig = ((HuaweiSourceConnector) connectorInst).getConfig(map);
    } catch (ClassNotFoundException e) {
      throw new ConnectException("Connector " + map.get("connector.class") + " not found", e);
    } catch (ReflectiveOperationException e) {
      throw new ConnectException("Connector " + map.get("connector.class")
          + " could not be instantiated", e);
    }
    this.initialize(connectorConfig, context.offsetStorageReader());
  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    // Only wait between polls when there is nothing left to catch up on, and wake up as soon
    // as the task is stopped.
    if (!lastPollHadRecords && stopLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      return null;
    }

    long requestsGenerated = 0;
    List<SourceRecord> sourceRecords = Collections.emptyList();

    try {
      Iterator<? extends RestRequest> requestIterator = this.requests().iterator();

      while (sourceRecords.isEmpty() && stopLatch.getCount() > 0 && requestIterator.hasNext()) {
        RestRequest request = requestIterator.next();

        logger.info("Requesting for user {}, url: {}",
            request.getUser().getUserId(), request.getRequest().url());
        requestsGenerated++;

        try {
          sourceRecords = this.handleRequest(request)
              .collect(Collectors.toList());
        } catch (IOException | RuntimeException ex) {
          logger.warn("Failed to make request: {}", ex.toString());
        }
      }
    } catch (Exception ex) {
      // Never let a failure to list users or build requests kill the task; retry next poll.
      logger.error("Failed to generate Huawei requests: {}", ex.toString(), ex);
    }

    lastPollHadRecords = !sourceRecords.isEmpty();
    logger.info("Processed {} records from {} URLs", sourceRecords.size(), requestsGenerated);

    return sourceRecords;
  }

  @Override
  public void stop() {
    logger.debug("Stopping source task");
    stopLatch.countDown();
  }

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }
}
