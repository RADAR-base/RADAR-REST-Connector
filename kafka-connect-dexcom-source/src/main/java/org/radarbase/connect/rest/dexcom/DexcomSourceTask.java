/*
 * Copyright 2018 The Hyve
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
 */

package org.radarbase.connect.rest.dexcom;

import io.confluent.connect.avro.AvroData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kotlin.streams.jdk8.StreamsKt;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.dexcom.offset.KafkaOffsetManager;
import org.radarbase.connect.rest.dexcom.user.DexcomUserRepository;
import org.radarbase.connect.rest.dexcom.util.VersionUtil;
import org.radarbase.dexcom.converter.TopicData;
import org.radarbase.dexcom.request.DexcomErrorBase;
import org.radarbase.dexcom.request.DexcomRequestGenerator;
import org.radarbase.dexcom.request.DexcomResult;
import org.radarbase.dexcom.request.RestRequest;
import org.radarbase.dexcom.route.DexcomCalibrationsRoute;
import org.radarbase.dexcom.route.DexcomEGVRoute;
import org.radarbase.dexcom.route.DexcomEventsRoute;
import org.radarbase.dexcom.route.Route;
import org.radarbase.dexcom.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DexcomSourceTask extends SourceTask {
  private static final Logger logger = LoggerFactory.getLogger(DexcomSourceTask.class);

  private OkHttpClient baseClient;
  private DexcomUserRepository userRepository;
  private List<Route> routes;
  private DexcomRequestGenerator dexcomRequestGenerator;
  private final AvroData avroData = new AvroData(20);
  private KafkaOffsetManager offsetManager;
  String TIMESTAMP_OFFSET_KEY = "timestamp";
  long TIMEOUT = 60000L;
  private int routeStartIndex = 0;

  public void initialize(
      DexcomRestSourceConnectorConfig config, OffsetStorageReader offsetStorageReader) {
    this.baseClient = new OkHttpClient();
    this.userRepository = config.getUserRepository();
    this.offsetManager = new KafkaOffsetManager(offsetStorageReader);
    this.routes = this.getRoutes(config);
    this.dexcomRequestGenerator =
        new DexcomRequestGenerator(this.userRepository, this.offsetManager, this.routes);
    this.offsetManager.initialize(getPartitions());
  }

  private List<Route> getRoutes(DexcomRestSourceConnectorConfig config) {
    List<Route> routes = new ArrayList<>();
    String apiBaseUrl = config.getDexcomApiBaseUrl();

    if (config.getDexcomEgvEnabled()) {
      routes.add(new DexcomEGVRoute(userRepository, apiBaseUrl));
    }
    if (config.getDexcomCalibrationEnabled()) {
      routes.add(new DexcomCalibrationsRoute(userRepository, apiBaseUrl));
    }
    if (config.getDexcomEventEnabled()) {
      routes.add(new DexcomEventsRoute(userRepository, apiBaseUrl));
    }
    return routes;
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

    List<Route> rotatedRoutes = getRotatedRoutes();
    return rotatedRoutes.stream()
        .flatMap((Route r) -> StreamsKt.asStream(dexcomRequestGenerator.requests(r, 100)));
  }

  private List<Route> getRotatedRoutes() {
    List<Route> rotatedRoutes = new ArrayList<>(this.routes);
    Collections.rotate(rotatedRoutes, routeStartIndex % this.routes.size());
    routeStartIndex = (routeStartIndex + 1) % this.routes.size();
    return rotatedRoutes;
  }

  @SuppressWarnings("unchecked")
  public Stream<SourceRecord> handleRequest(RestRequest req) throws IOException {
    try (Response response = baseClient.newCall(req.getRequest()).execute()) {
      DexcomResult<?> result = this.dexcomRequestGenerator.handleResponse(req, response);
      if (result instanceof DexcomResult.Success) {
        DexcomResult.Success<List<TopicData>> success =
            (DexcomResult.Success<List<TopicData>>) result;
        return success.getValue().stream()
            .map(
                r -> {
                  SchemaAndValue avro =
                      avroData.toConnectData(r.getValue().getSchema(), r.getValue());
                  SchemaAndValue key = avroData.toConnectData(r.getKey().getSchema(), r.getKey());
                  Map<String, Object> partition =
                      getPartition(req.getRoute().toString(), req.getUser());
                  Map<String, ?> offset =
                      Collections.singletonMap(TIMESTAMP_OFFSET_KEY, r.getOffset());

                  return new SourceRecord(
                      partition,
                      offset,
                      r.getTopic(),
                      key.schema(),
                      key.value(),
                      avro.schema(),
                      avro.value());
                });
      } else {
        DexcomErrorBase e = (DexcomErrorBase) ((DexcomResult.Error) result).getError();
        logger.warn(
            "Failed to make request: {} {} {}",
            e.getMessage(),
            e.getCause() != null ? e.getCause().toString() : "null",
            e.getCode());
        return Stream.empty();
      }
    }
  }

  @Override
  public void start(Map<String, String> map) {
    DexcomRestSourceConnectorConfig connectorConfig = new DexcomRestSourceConnectorConfig(map);
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

        logger.info(
            "Requesting for user {}, url: {}",
            request.getUser().getUserId(),
            request.getRequest().url());
        requestsGenerated++;

        try {
          sourceRecords = this.handleRequest(request).collect(Collectors.toList());
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
