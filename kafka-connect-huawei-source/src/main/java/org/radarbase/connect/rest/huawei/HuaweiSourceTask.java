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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  private KafkaOffsetManager offsetManager;
  private static final String TIMESTAMP_OFFSET_KEY = "timestamp";
  private static final long TIMEOUT = 60000L;
  private int routeStartIndex = 0;

  public void initialize(HuaweiRestSourceConnectorConfig config, OffsetStorageReader offsetStorageReader) {
    this.baseClient = new OkHttpClient();

    this.userRepository = config.getUserRepository();
    this.offsetManager = new KafkaOffsetManager(offsetStorageReader);
    this.routes = getRoutes(config);
    this.huaweiRequestGenerator = new HuaweiRequestGenerator(this.userRepository, this.offsetManager, this.routes);
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
      HuaweiResult<List<TopicData>> result = this.huaweiRequestGenerator.handleResponse(req, response);
      if (result instanceof HuaweiResult.Success) {
        Success<List<TopicData>> success = (Success<List<TopicData>>) result;
        return success.getValue().stream().map(r -> {
          SchemaAndValue avro = avroData.toConnectData(r.getValue().getSchema(), r.getValue());
          SchemaAndValue key = avroData.toConnectData(r.getKey().getSchema(), r.getKey());
          Map<String, Object> partition = getPartition(req.getRoute().toString(), req.getUser());
          Map<String, ?> offset = Collections.singletonMap(TIMESTAMP_OFFSET_KEY, r.getOffset());

          return new SourceRecord(partition, offset, r.getTopic(),
                key.schema(), key.value(), avro.schema(), avro.value());
        });
      } else {
        HuaweiErrorBase e = (HuaweiErrorBase) ((HuaweiResult.Error) result).getError();
        logger.warn("Failed to make request: {} {} {}", e.getMessage(), e.getCause(), e.getCode());
        return Stream.empty();
      }
    }
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
    long requestsGenerated = 0;
    List<SourceRecord> sourceRecords = Collections.emptyList();

    do {
      Thread.sleep(TIMEOUT);

      Iterator<? extends RestRequest> requestIterator = this.requests().iterator();

      while (sourceRecords.isEmpty() && requestIterator.hasNext()) {
        RestRequest request = requestIterator.next();

        logger.info("Requesting for user {}, url: {}", request.getUser().getUserId(), request.getRequest().url());
        requestsGenerated++;

        try {
          sourceRecords = this.handleRequest(request)
              .collect(Collectors.toList());
        } catch (IOException ex) {
          logger.warn("Failed to make request: {}", ex.toString());
        }
      }
    } while (sourceRecords.isEmpty());

    logger.info("Processed {} records from {} URLs", sourceRecords.size(), requestsGenerated);

    return sourceRecords;
  }

  @Override
  public void stop() {
    logger.debug("Stopping source task");
  }

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }
}
