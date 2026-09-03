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

package org.radarbase.connect.rest.dexcom;

import static org.radarbase.connect.rest.dexcom.DexcomRestSourceConnectorConfig.DEXCOM_USERS_CONFIG;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;
import kotlin.streams.jdk8.StreamsKt;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.radarbase.connect.rest.dexcom.user.DexcomUserRepository;
import org.radarbase.dexcom.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DexcomSourceConnector extends AbstractRestSourceConnector {

  private static final Logger logger = LoggerFactory.getLogger(DexcomSourceConnector.class);
  private ScheduledExecutorService executor;
  private Set<? extends User> configuredUsers;
  private DexcomUserRepository repository;

  @Override
  public void start(Map<String, String> props) {
    super.start(props);
    executor = Executors.newSingleThreadScheduledExecutor();

    executor.scheduleAtFixedRate(() -> {
      if (repository.hasPendingUpdates()) {
        try {
          logger.info("Requesting latest user details...");
          repository.applyPendingUpdates();
          Set<? extends User> newUsers =
              SequencesKt.toSet(getConfig(props, false).getUserRepository(repository).stream());
          if (configuredUsers != null && !newUsers.equals(configuredUsers)) {
            logger.info("User info mismatch found. Requesting reconfiguration...");
            reconfigure();
          }
        } catch (IOException e) {
          logger.warn("Failed to refresh users: {}", e.toString());
        }
      } else {
        logger.info("No pending updates found. Not attempting to refresh users.");
      }
    }, 0, 5, TimeUnit.MINUTES);
  }

  @Override
  public void stop() {
    super.stop();
    executor.shutdown();

    configuredUsers = null;
  }

  private DexcomRestSourceConnectorConfig getConfig(Map<String, String> conf, boolean doLog) {
    return new DexcomRestSourceConnectorConfig(conf, doLog);
  }

  public DexcomRestSourceConnectorConfig getConfig(Map<String, String> conf) {
    DexcomRestSourceConnectorConfig connectorConfig = getConfig(conf, true);
    repository = connectorConfig.getUserRepository(repository);
    return connectorConfig;
  }

  @Override
  public ConfigDef config() {
    return DexcomRestSourceConnectorConfig.conf();
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    return configureTasks(maxTasks);
  }

  private List<Map<String, String>> configureTasks(int maxTasks) {
    Map<String, String> baseConfig = config.originalsStrings();
    DexcomRestSourceConnectorConfig dexcomConfig = getConfig(baseConfig);
    if (repository == null) {
      repository = dexcomConfig.getUserRepository(null);
    }
    // Divide the users over tasks
    try {
      Sequence<String> ids =
          SequencesKt.map(
              dexcomConfig.getUserRepository(repository).stream(), u -> u.getVersionedId());
      List<Map<String, String>> userTasks =
          StreamsKt.asStream(ids)
              // group users based on their hashCode, in principle, this allows for more efficient
              // reconfigurations for a fixed number of tasks, since that allows existing tasks to
              // only handle small modifications users to handle.
              .collect(
                  Collectors.groupingBy(
                      u -> Math.abs(u.hashCode()) % maxTasks, Collectors.joining(",")))
              .values()
              .stream()
              .map(
                  u -> {
                    Map<String, String> config = new HashMap<>(baseConfig);
                    config.put(DEXCOM_USERS_CONFIG, u);
                    return config;
                  })
              .collect(Collectors.toList());
      this.configuredUsers = SequencesKt.toSet(dexcomConfig.getUserRepository().stream());
      logger.info("Received userTask Configs {}", userTasks);
      return userTasks;
    } catch (Exception ex) {
      throw new ConfigException("Cannot read users", ex);
    }
  }

  public void reconfigure() {
    new Thread(
            () -> {
              logger.info("Requesting reconfiguration");
              context.requestTaskReconfiguration();
              logger.info("Requested reconfiguration");
            })
        .start();
  }
}
