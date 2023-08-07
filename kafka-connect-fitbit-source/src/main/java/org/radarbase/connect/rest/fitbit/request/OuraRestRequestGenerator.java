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

package org.radarbase.connect.rest.fitbit.request;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.confluent.connect.avro.AvroData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import okhttp3.OkHttpClient;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.route.FitbitActivityLogRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitIntradayCaloriesRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitIntradayHeartRateRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitIntradayStepsRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitRestingHeartRateRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitSleepRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitTimeZoneRoute;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.request.RequestGeneratorRouter;
import org.radarbase.connect.rest.request.RequestRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.radarbase.oura.request.OuraRequestGenerator;
import org.radarbase.oura.request.RestRequest;
import org.radarbase.oura.user.UserRepository;
import org.radarbase.connect.rest.fitbit.user.ServiceUserRepository;
import kotlin.sequences.*;

/**
 * Generate all requests for Fitbit API.
 */
public class OuraRestRequestGenerator {
  public static final JsonFactory JSON_FACTORY = new JsonFactory();
  public static final ObjectReader JSON_READER = new ObjectMapper(JSON_FACTORY)
      .registerModule(new JavaTimeModule())
      .reader();
  private static final Logger logger = LoggerFactory.getLogger(OuraRestRequestGenerator.class);

  private OkHttpClient baseClient;
  private final Map<String, OkHttpClient> clients;
  private UserRepository userRepository;
  private List<RequestRoute> routes;

  private OuraRequestGenerator ouraRequestGenerator;

  public OuraRestRequestGenerator() {
    clients = new HashMap<>();
  }

  public Stream<RequestRoute> routes() {
    return this.routes.stream();
  }

  public Stream<RestRequest> requests() {
    return userRepository.stream().flatMap(user -> ouraRequestGenerator.requests(user, 100).stream());    
  }

  public void initialize(RestSourceConnectorConfig config) {
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    this.baseClient = new OkHttpClient();

    this.userRepository = new ServiceUserRepository();
    this.routes = getRoutes(fitbitConfig);
    this.ouraRequestGenerator = new OuraRequestGenerator(userRepository, Duration.ofDays(15), baseClient);

    logger.info("Initializing..");

    // super.initialize(config);
  }

  private List<RequestRoute> getRoutes(FitbitRestSourceConnectorConfig config) {
    AvroData avroData = new AvroData(20);
    List<RequestRoute> localRoutes = new ArrayList<>(5);
    logger.info("Getting routes..");

    return localRoutes;
  }

  public OkHttpClient getClient(User user) {
    logger.info("Getting client..");

    return clients.computeIfAbsent(user.getId(), u -> baseClient.newBuilder()
        .authenticator(new TokenAuthenticator(user, userRepository))
        .build());
  }

  public Map<String, Map<String, Object>> getPartitions(String route) {
    try {
      return userRepository.stream()
          .collect(Collectors.toMap(User::getVersionedId, u -> getPartition(route, u)));
    } catch (IOException e) {
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
}
