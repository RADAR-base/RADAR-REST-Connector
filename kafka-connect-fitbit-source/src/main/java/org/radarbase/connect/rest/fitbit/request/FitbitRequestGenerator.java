package org.radarbase.connect.rest.fitbit.request;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.confluent.connect.avro.AvroData;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.route.FitbitIntradayStepsRoute;
import org.radarbase.connect.rest.fitbit.route.FitbitSleepRoute;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;
import org.radarbase.connect.rest.request.RequestGeneratorRouter;
import org.radarbase.connect.rest.request.RequestRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitRequestGenerator extends RequestGeneratorRouter {
  public static final JsonFactory JSON_FACTORY = new JsonFactory();
  public static final ObjectReader JSON_READER = new ObjectMapper(JSON_FACTORY).reader();
  private static final Logger logger = LoggerFactory.getLogger(FitbitRequestGenerator.class);

  private OkHttpClient baseClient;
  private final Map<String, OkHttpClient> clients;
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

    AvroData avroData = new AvroData(20);
    this.userRepository = config1.getFitbitUserRepository();
    this.routes = Arrays.asList(
        new FitbitIntradayStepsRoute(this, userRepository, avroData),
        new FitbitSleepRoute(this, userRepository, avroData)
    );

    super.initialize(config);
  }

  public OkHttpClient getClient(FitbitUser user) {
    return clients.computeIfAbsent(user.getId(), u -> baseClient.newBuilder()
          .authenticator(new TokenAuthenticator(user, userRepository))
          .build());
  }

  public Map<String, Map<String, Object>> getPartitions(String route) {
    try {
      return userRepository.stream()
          .collect(Collectors.toMap(
              FitbitUser::getId,
              u -> getPartition(route, u)));
    } catch (IOException e) {
      logger.warn("Failed to initialize user partitions for route {}: {}", route, e.toString());
      return Collections.emptyMap();
    }
  }

  public Map<String, Object> getPartition(String route, FitbitUser user) {
    Map<String, Object> partition = new HashMap<>(4);
    partition.put("user", user.getId());
    partition.put("route", route);
    return partition;
  }
}
