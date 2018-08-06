package org.radarbase.connect.rest.fitbit.config;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.util.ConnectorUtils;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.config.TaskWorkDivision;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitUser;
import org.radarbase.connect.rest.fitbit.FitbitUserRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig.FITBIT_USERS_CONFIG;

public class FitbitUserTaskDivision implements TaskWorkDivision {
  private Map<String, String> baseConfig;
  private FitbitUserRepository userRepository;

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    baseConfig = fitbitConfig.originalsStrings();
    userRepository = fitbitConfig.getFitbitUserRepository();

  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    try {
      return userRepository.stream()
          .map(FitbitUser::getKey)
          .collect(Collectors.groupingBy(
              u -> Math.abs(u.hashCode()) % maxTasks,
              Collectors.joining(",")))
          .values().stream()
          .map(u -> {
            Map<String, String> config = new HashMap<>(baseConfig);
            config.put(FITBIT_USERS_CONFIG, u);
            return config;
          })
          .collect(Collectors.toList());
    } catch (IOException ex) {
      throw new ConfigException("Cannot read users", ex);
    }
  }
}
