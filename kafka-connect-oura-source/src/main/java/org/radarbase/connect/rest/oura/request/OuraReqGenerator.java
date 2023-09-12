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

package org.radarbase.connect.rest.oura.request;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.confluent.connect.avro.AvroData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.oura.OuraRestSourceConnectorConfig;
import org.radarbase.connect.rest.oura.offset.KafkaOffsetManager;
import org.radarbase.oura.user.User;
import org.radarbase.connect.rest.request.RequestGeneratorRouter;
import org.radarbase.connect.rest.request.RequestRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.radarbase.oura.request.OuraRequestGenerator;
import org.radarbase.connect.rest.oura.user.OuraServiceUserRepository;
import org.radarbase.oura.request.RestRequest;
import java.time.Instant;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.oura.route.Route;
import kotlin.streams.jdk8.StreamsKt;
import kotlin.sequences.SequencesKt;
import kotlin.sequences.Sequence;

/**
 * Generate all requests for Oura API.
 */
public class OuraReqGenerator {
  public static final JsonFactory JSON_FACTORY = new JsonFactory();
  public static final ObjectReader JSON_READER = new ObjectMapper(JSON_FACTORY)
      .registerModule(new JavaTimeModule())
      .reader();
  private static final Logger logger = LoggerFactory.getLogger(OuraReqGenerator.class);

  private OkHttpClient baseClient;
  private final Map<String, OkHttpClient> clients;
  private OuraServiceUserRepository userRepository;
  private List<Route> routes = this.ouraRequestGenerator.getRoutes();
  private OuraRequestGenerator ouraRequestGenerator; 
  private AvroData avroData = new AvroData(20);
  private KafkaOffsetManager offsetManager;


  public OuraReqGenerator() {
    clients = new HashMap<>();
  }

  public void initialize(RestSourceConnectorConfig config, OffsetStorageReader offsetStorageReader) {
    OuraRestSourceConnectorConfig ouraConfig = (OuraRestSourceConnectorConfig) config;
    this.baseClient = new OkHttpClient();

    this.userRepository = ouraConfig.getUserRepository();
    this.offsetManager = new KafkaOffsetManager(offsetStorageReader, null);
    this.ouraRequestGenerator = new OuraRequestGenerator(this.userRepository, null);
  }

  public OkHttpClient getClient(User user) {
    return clients.computeIfAbsent(user.getId(), u -> baseClient.newBuilder()
        .authenticator(new TokenAuthenticator(user, userRepository))
        .build());
  }

  public Map<String, Map<String, Object>> getPartitions(String route) {
    try {
      return StreamsKt.asStream(userRepository.stream())
          .collect(Collectors.toMap(User::getVersionedId, u -> getPartition(route, u)));
    } catch (Exception e) {
      logger.warn("Failed to initialize user partitions for route {}: {}", route, e.toString());
      return Collections.emptyMap();
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

  public Instant getTimeOfNextRequest() {
    // Get from routes
    return Instant.MIN;
  }

  public Stream<SourceRecord> handleRequest(RestRequest req) throws IOException {
    Collection<SourceRecord> records;

    try (Response response = baseClient.newCall(req.getRequest()).execute()) {
      return this.ouraRequestGenerator.handleResponse(req, response).stream().map(r -> {
        SchemaAndValue avro = avroData.toConnectData(r.getValue().getSchema(), r.getValue());
        SchemaAndValue key = avroData.toConnectData(r.getKey().getSchema(), r.getKey());

        // Fix offsets
        return new SourceRecord(null, null, r.getTopic(),
              key.schema(), key.value(), avro.schema(), avro.value());
      });
    } catch (IOException ex) {
      throw ex;
    }
  }

}