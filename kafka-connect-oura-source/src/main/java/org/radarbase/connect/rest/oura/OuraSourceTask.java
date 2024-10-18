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
 *
 */

package org.radarbase.connect.rest.oura;

import static java.time.temporal.ChronoUnit.MILLIS;

import java.io.IOException;
import java.time.Instant;
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
import org.radarbase.connect.rest.oura.offset.KafkaOffsetManager;
import org.radarbase.connect.rest.oura.user.OuraUserRepository;
import org.radarbase.connect.rest.oura.util.VersionUtil;
import org.radarbase.oura.converter.TopicData;
import org.radarbase.oura.request.OuraRequestGenerator;
import org.radarbase.oura.request.OuraResult;
import org.radarbase.oura.request.OuraResult.Success;
import org.radarbase.oura.request.OuraResult.Error;
import org.radarbase.oura.request.OuraErrorBase;
import org.radarbase.oura.request.RestRequest;
import org.radarbase.oura.route.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.radarbase.oura.user.User;
import io.confluent.connect.avro.AvroData;
import kotlin.streams.jdk8.StreamsKt;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class OuraSourceTask extends SourceTask {
  private static final Logger logger = LoggerFactory.getLogger(OuraSourceTask.class);

  private OkHttpClient baseClient;
  private OuraUserRepository userRepository;
  private List<Route> routes;
  private OuraRequestGenerator ouraRequestGenerator;
  private AvroData avroData = new AvroData(20);
  private KafkaOffsetManager offsetManager;
  String TIMESTAMP_OFFSET_KEY = "timestamp";
  long TIMEOUT = 60000L;

  public void initialize(OuraRestSourceConnectorConfig config, OffsetStorageReader offsetStorageReader) {
    OuraRestSourceConnectorConfig ouraConfig = (OuraRestSourceConnectorConfig) config;
    this.baseClient = new OkHttpClient();

    this.userRepository = ouraConfig.getUserRepository();
    this.offsetManager = new KafkaOffsetManager(offsetStorageReader);
    this.ouraRequestGenerator = new OuraRequestGenerator(this.userRepository, this.offsetManager);
    this.routes = this.ouraRequestGenerator.getRoutes();
    this.offsetManager.initialize(getPartitions());
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
    Stream<Route> routes = this.routes.stream();
    return routes.flatMap((Route r) -> StreamsKt.asStream(ouraRequestGenerator.requests(r, 100)));
  }

  public Stream<SourceRecord> handleRequest(RestRequest req) throws IOException {
    try (Response response = baseClient.newCall(req.getRequest()).execute()) {
      OuraResult result = this.ouraRequestGenerator.handleResponse(req, response);
      if (result instanceof OuraResult.Success) {
        OuraResult.Success<List<TopicData>> success = (Success<List<TopicData>>) result;
        return success.getValue().stream().map(r -> {
          SchemaAndValue avro = avroData.toConnectData(r.getValue().getSchema(), r.getValue());
          SchemaAndValue key = avroData.toConnectData(r.getKey().getSchema(), r.getKey());
          Map<String, Object> partition = getPartition(req.getRoute().toString(), req.getUser());
          Map<String, ?> offset = Collections.singletonMap(TIMESTAMP_OFFSET_KEY, r.getOffset());

          return new SourceRecord(partition, offset, r.getTopic(),
                key.schema(), key.value(), avro.schema(), avro.value());
        });
      } else {
        OuraErrorBase e = (OuraErrorBase) ((OuraResult.Error) result).getError();
        logger.warn("Failed to make request: {} {} {}", e.getMessage(), e.getCause().toString(), e.getCode());
        return Stream.empty();
      }
    } catch (IOException ex) {
      throw ex;
    }
  }

  @Override
  public void start(Map<String, String> map) {
    OuraRestSourceConnectorConfig connectorConfig;
    try {
      Class<?> connector = Class.forName(map.get("connector.class"));
      Object connectorInst = connector.getConstructor().newInstance();
      connectorConfig = ((OuraSourceConnector)connectorInst).getConfig(map);
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

      Map<String, String> configs = context.configs();
      Iterator<? extends RestRequest> requestIterator = this.requests()
          .iterator();


      while (sourceRecords.isEmpty() && requestIterator.hasNext()) {
        RestRequest request = requestIterator.next();

        logger.info("Requesting {}", request.getRequest().url());
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