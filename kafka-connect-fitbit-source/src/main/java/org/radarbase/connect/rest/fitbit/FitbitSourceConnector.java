package org.radarbase.connect.rest.fitbit;

import static org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig.FITBIT_USERS_CONFIG;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.radarbase.connect.rest.AbstractRestSourceConnector;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;

public class FitbitSourceConnector extends AbstractRestSourceConnector {
  @Override
  public FitbitRestSourceConnectorConfig getConfig(Map<String, String> conf) {
    return new FitbitRestSourceConnectorConfig(conf);
  }

  @Override
  public ConfigDef config() {
    return FitbitRestSourceConnectorConfig.conf();
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    Map<String, String> baseConfig = config.originalsStrings();
    FitbitRestSourceConnectorConfig fitbitConfig = getConfig(baseConfig);
    try {
      return fitbitConfig.getFitbitUserRepository().stream()
          .map(FitbitUser::getId)
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
