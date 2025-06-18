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
import org.radarbase.connect.rest.fitbit.route.*;
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
  private UserRepository userRepository;
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

    this.userRepository = fitbitConfig.getUserRepository();
    this.routes = getRoutes(fitbitConfig);

    super.initialize(config);
  }

  private List<RequestRoute> getRoutes(FitbitRestSourceConnectorConfig config) {
    AvroData avroData = new AvroData(20);
    List<RequestRoute> localRoutes = new ArrayList<>(5);

    if (config.getFitbitSleepStagesEnabled() || config.getFitbitSleepClassicEnabled()) {
      localRoutes.add(new FitbitSleepRoute(this, userRepository, avroData));
    }

    if (config.getFitbitTimeZoneEnabled()) {
      localRoutes.add(new FitbitTimeZoneRoute(this, userRepository, avroData));
    }

    if (config.getFitbitActivityLogEnabled()) {
      localRoutes.add(new FitbitActivityLogRoute(this, userRepository, avroData));
    }

    if (config.getFitbitRestingHeartRateEnabled()) {
      localRoutes.add(new FitbitRestingHeartRateRoute(this, userRepository, avroData));
    }

    if (config.hasIntradayAccess()) {

      if (config.getFitbitIntradayStepsEnabled()) {
        localRoutes.add(new FitbitIntradayStepsRoute(this, userRepository, avroData));
      }
      if (config.getFitbitIntradayHeartRateEnabled()) {
        localRoutes.add(new FitbitIntradayHeartRateRoute(this, userRepository, avroData));
      }
      if (config.getFitbitIntradayHeartRateVariabilityEnabled()) {
        localRoutes.add(new FitbitIntradayHeartRateVariabilityRoute(this, userRepository, avroData));
      }
      if (config.getFitbitBreathingRateEnabled()) {
        localRoutes.add(new FitbitBreathingRateRoute(this, userRepository, avroData));
      }
      if (config.getFitbitSkinTemperatureEnabled()) {
        localRoutes.add(new FitbitSkinTemperatureRoute(this, userRepository, avroData));
      }
      if (config.getFitbitIntradayCaloriesEnabled()) {
        localRoutes.add(new FitbitIntradayCaloriesRoute(this, userRepository, avroData));
      }
      if (config.getFitbitIntradaySpo2Enabled()) {
        localRoutes.add(new FitbitIntradaySpo2Route(this, userRepository, avroData));
      }
    }
    return localRoutes;
  }

  public OkHttpClient getClient(User user) {
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
