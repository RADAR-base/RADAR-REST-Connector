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

package org.radarbase.connect.rest.fitbit;

import static java.util.stream.Collectors.toSet;
import static org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig.FITBIT_USERS_CONFIG;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.radarbase.connect.rest.AbstractRestSourceConnector;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitSourceConnector extends AbstractRestSourceConnector {

  private static final Logger logger = LoggerFactory.getLogger(FitbitSourceConnector.class);
  private ScheduledExecutorService executor;
  private Set<? extends User> configuredUsers;


  @Override
  public void start(Map<String, String> props) {
    super.start(props);
    executor = Executors.newSingleThreadScheduledExecutor();

    executor.scheduleAtFixedRate(() -> {
      Set<? extends User> newUsers = new HashSet<>();
      try {
        logger.info("Requesting latest user details...");
        for(UserRepository u: getConfig(props, false).getUserRepositories()) {
          Set users = u.stream().collect(toSet());
          newUsers.addAll(users);
        }
        if (configuredUsers != null && !newUsers.equals(configuredUsers)) {
          logger.info("User info mismatch found. Requesting reconfiguration...");
          reconfigure();
        }
      } catch (IOException e) {
        logger.warn("Failed to refresh users: {}", e.toString());
      }
    },0, 5, TimeUnit.MINUTES);
  }

  @Override
  public void stop() {
    super.stop();
    executor.shutdown();

    configuredUsers = null;
  }

  private FitbitRestSourceConnectorConfig getConfig(Map<String, String> conf, boolean doLog) {
    return new FitbitRestSourceConnectorConfig(conf, doLog);
  }

  @Override
  public FitbitRestSourceConnectorConfig getConfig(Map<String, String> conf) {
    return getConfig(conf, true);
  }

  @Override
  public ConfigDef config() {
    return FitbitRestSourceConnectorConfig.conf();
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    return configureTasks(maxTasks);
  }

  private List<Map<String, String>> configureTasks(int maxTasks) {
    Map<String, String> baseConfig = config.originalsStrings();
    FitbitRestSourceConnectorConfig fitbitConfig = getConfig(baseConfig);
    // Divide the users over tasks
    List<Map<String, String>> userTasks = new ArrayList<>();
    this.configuredUsers = new HashSet<>();

    try {
      for(UserRepository ur: fitbitConfig.getUserRepositories()) {
        List<Map<String, String>> userTasksCurrent = ur.stream()
            .map(User::getId)
            // group users based on their hashCode
            // in principle this allows for more efficient reconfigurations for a fixed number of tasks,
            // since that allows existing tasks to only handle small modifications users to handle.
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
        userTasks.addAll(userTasksCurrent);
        Set currentUsers = ur.stream()
            .collect(toSet());
        this.configuredUsers.addAll(currentUsers);
        logger.info("Received userTask Configs {}", userTasks);
      }
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
}
