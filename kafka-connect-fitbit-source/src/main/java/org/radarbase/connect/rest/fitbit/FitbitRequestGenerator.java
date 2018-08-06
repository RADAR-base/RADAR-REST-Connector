package org.radarbase.connect.rest.fitbit;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.route.FitbitIntradayStepsRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitSleepRoute;
import org.radarbase.connect.rest.request.RequestGeneratorRouter;
import org.radarbase.connect.rest.request.RequestRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FitbitRequestGenerator extends RequestGeneratorRouter {
  public static final JsonFactory JSON_FACTORY = new JsonFactory();
  public static final ObjectReader JSON_READER = new ObjectMapper(JSON_FACTORY).reader();
  private static final Logger logger = LoggerFactory.getLogger(FitbitRequestGenerator.class);

  private OkHttpClient baseClient;
  private final Map<String, OkHttpClient> clients;
  private Headers clientCredentials;
  private FitbitUserRepository userRepository;
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
    FitbitRestSourceConnectorConfig config1 = (FitbitRestSourceConnectorConfig) config;
    this.baseClient = new OkHttpClient();

    String credentialString = config1.getFitbitClient() + ":"
        + config1.getFitbitClientSecret();
    String credentialsBase64 = Base64.getEncoder().encodeToString(
        credentialString.getBytes(StandardCharsets.UTF_8));

    this.clientCredentials = Headers.of("Authorization", "Basic " + credentialsBase64);
    this.userRepository = config1.getFitbitUserRepository();
    this.routes = Arrays.asList(
        new FitbitIntradayStepsRoute(this, userRepository),
        new FitbitSleepRoute(this, userRepository)
    );
  }

  public OkHttpClient getClient(FitbitUser user) {
    return clients.computeIfAbsent(user.getKey(), u -> baseClient.newBuilder()
          .authenticator(new TokenAuthenticator(user, clientCredentials, baseClient, userRepository))
          .build());
  }

  public Map<String, Map<String, Object>> getPartitions(String route) {
    try {
      return userRepository.stream()
          .collect(Collectors.toMap(
              FitbitUser::getKey,
              u -> getPartition(route, u)));
    } catch (IOException e) {
      logger.warn("Failed to initialize user partitions for route {}: {}", route, e.toString());
      return Collections.emptyMap();
    }
  }

  public Map<String, Object> getPartition(String route, FitbitUser user) {
    Map<String, Object> partition = new HashMap<>(4);
    partition.put("user", user.getKey());
    partition.put("route", route);
    return partition;
  }
}
