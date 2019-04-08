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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.route.FitbitActivityLogRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitIntradayHeartRateRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitIntradayStepsRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitSleepRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitTimeZoneRoute;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.connect.rest.request.RequestGeneratorRouter;
import org.radarbase.connect.rest.request.RequestRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate all requests for Fitbit API.
 */
public class FitbitRequestGenerator extends RequestGeneratorRouter {
  public static final JsonFactory JSON_FACTORY = new JsonFactory();
  public static final ObjectReader JSON_READER = new ObjectMapper(JSON_FACTORY)
      .registerModule(new JavaTimeModule())
      .reader();
  private static final Logger logger = LoggerFactory.getLogger(FitbitRequestGenerator.class);

  private OkHttpClient baseClient;
  private final Map<String, OkHttpClient> clients;
  private List<UserRepository> userRepositories;
  private List<RequestRoute> routes;

  public FitbitRequestGenerator() {
    clients = new HashMap<>();
  }

  @Override
  public Stream<RequestRoute> routes() {
    return this.routes.stream();
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    this.baseClient = new OkHttpClient();

    this.userRepositories = fitbitConfig.getUserRepositories();
    this.routes = getRoutes(fitbitConfig);

    super.initialize(config);
  }

  private List<RequestRoute> getRoutes(FitbitRestSourceConnectorConfig config) {
    AvroData avroData = new AvroData(20);
    List<RequestRoute> localRoutes = new ArrayList<>(5 * userRepositories.size());
    for(UserRepository userRepository : userRepositories) {
      localRoutes.add(new FitbitSleepRoute(this, userRepository, avroData));
      localRoutes.add(new FitbitTimeZoneRoute(this, userRepository, avroData));
      localRoutes.add(new FitbitActivityLogRoute(this, userRepository, avroData));
      if (config.hasIntradayAccess()) {
        localRoutes.add(new FitbitIntradayStepsRoute(this, userRepository, avroData));
        localRoutes.add(new FitbitIntradayHeartRateRoute(this, userRepository, avroData));
      }
    }
    return localRoutes;
  }

  public OkHttpClient getClient(User user, UserRepository userRepository) {

    return clients.computeIfAbsent(user.getId(), u -> baseClient.newBuilder()
        .authenticator(new TokenAuthenticator(user, userRepository))
        .build());
  }

  public Map<String, Map<String, Object>> getPartitions(String route) {
    try {
      Map<String, Map<String, Object>> partitions = new HashMap<>();
      for(UserRepository userRepository : userRepositories) {
        partitions.putAll(userRepository.stream()
            .collect(Collectors.toMap(User::getId, u -> getPartition(route, u))));
      }
      return partitions;
    } catch (IOException e) {
      logger.warn("Failed to initialize user partitions for route {}: {}", route, e.toString());
      return Collections.emptyMap();
    }
  }

  public Map<String, Object> getPartition(String route, User user) {
    Map<String, Object> partition = new HashMap<>(4);
    partition.put("user", user.getId());
    partition.put("route", route);
    return partition;
  }
}
