package org.radarbase.connect.rest.garmin;

import static org.radarbase.connect.rest.garmin.GarminRestSourceConnectorConfig.GARMIN_USERS_CONFIG;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.radarbase.connect.rest.AbstractRestSourceConnector;
import org.radarbase.connect.rest.garmin.user.User;
import org.radarbase.connect.rest.garmin.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GarminSourceConnector extends AbstractRestSourceConnector {

  private static final Logger logger = LoggerFactory.getLogger(GarminSourceConnector.class);

  private GarminHttpServer httpServer;
  private UserRepository userRepository;
  private Set<User> configuredUsers;

  @Override
  public void stop() {
    super.stop();
    logger.info("Stopping Garmin HTTP Server");
    httpServer.stop();
  }

  @Override
  public void start(Map<String, String> props) {
    super.start(props);

    // Configure the tasks first

    httpServer = new GarminHttpServer();
    httpServer.initialize((GarminRestSourceConnectorConfig) config);
    httpServer.start();
  }

  @Override
  public Class<? extends Task> taskClass() {
    return GarminSourceTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    return configureTasks(maxTasks);
  }

  private List<Map<String, String>> configureTasks(int maxTasks) {
    Map<String, String> baseConfig = config.originalsStrings();
    GarminRestSourceConnectorConfig garminConfig = getConfig(baseConfig);

    // Divide the users over tasks
    try {
      List<Map<String, String>> userTasks = garminConfig.getUserRepository(userRepository).stream()
          .map(User::getVersionedId)
          // group users based on their hashCode, in principle, this allows for more efficient
          // reconfigurations for a fixed number of tasks, since that allows existing tasks to
          // only handle small modifications users to handle.
          .collect(Collectors.groupingBy(
              u -> Math.abs(u.hashCode()) % maxTasks,
              Collectors.joining(",")))
          .values().stream()
          .map(u -> {
            Map<String, String> config = new HashMap<>(baseConfig);
            config.put(GARMIN_USERS_CONFIG, u);
            return config;
          })
          .collect(Collectors.toList());
      this.configuredUsers = garminConfig.getUserRepository(userRepository).stream()
          .collect(Collectors.toSet());
      logger.info("Received userTask Configs {}", userTasks);
      return userTasks;
    } catch (IOException ex) {
      throw new ConfigException("Cannot read users", ex);
    }
  }

  public void reconfigure() {
    new Thread(() -> {
      logger.info("Requesting reconfiguration");
      context.requestTaskReconfiguration();
      logger.info("Requested reconfiguration");
    }).start();
  }

  @Override
  public GarminRestSourceConnectorConfig getConfig(Map<String, String> conf) {
    return getConfig(conf, true);
  }

  private GarminRestSourceConnectorConfig getConfig(Map<String, String> conf, boolean doLog) {
    return new GarminRestSourceConnectorConfig(conf, doLog);
  }

  @Override
  public ConfigDef config() {
    return GarminRestSourceConnectorConfig.conf();
  }

}
