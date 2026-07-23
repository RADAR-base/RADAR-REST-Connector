/*
 * Copyright 2026 Onsentia
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

package org.radarbase.connect.rest.huawei;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.radarbase.huawei.user.User;
import org.radarbase.connect.rest.huawei.user.HuaweiUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kotlin.sequences.SequencesKt;
import kotlin.sequences.Sequence;
import kotlin.streams.jdk8.StreamsKt;

import static org.radarbase.connect.rest.huawei.HuaweiRestSourceConnectorConfig.HUAWEI_USERS_CONFIG;

public class HuaweiSourceConnector extends AbstractRestSourceConnector {

  private static final Logger logger = LoggerFactory.getLogger(HuaweiSourceConnector.class);
  private ScheduledExecutorService executor;
  private Set<? extends User> configuredUsers;
  private HuaweiUserRepository repository;

  @Override
  public void start(Map<String, String> props) {
    logger.info("Starting Huawei source connector");
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

  private HuaweiRestSourceConnectorConfig getConfig(Map<String, String> conf, boolean doLog) {
    return new HuaweiRestSourceConnectorConfig(conf, doLog);
  }

  @Override
  public HuaweiRestSourceConnectorConfig getConfig(Map<String, String> conf) {
    HuaweiRestSourceConnectorConfig connectorConfig = getConfig(conf, true);
    repository = connectorConfig.getUserRepository(repository);
    return connectorConfig;
  }

  @Override
  public ConfigDef config() {
    return HuaweiRestSourceConnectorConfig.conf();
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    return configureTasks(maxTasks);
  }

  private List<Map<String, String>> configureTasks(int maxTasks) {
    Map<String, String> baseConfig = config.originalsStrings();
    HuaweiRestSourceConnectorConfig huaweiConfig = getConfig(baseConfig);
    if (repository == null) {
      repository = huaweiConfig.getUserRepository(null);
    }
    // Divide the users over tasks
    try {
      Sequence<String> ids = SequencesKt.map(huaweiConfig.getUserRepository(repository).stream(), User::getVersionedId);
      List<Map<String, String>> userTasks = StreamsKt.asStream(ids)
          // group users based on their hashCode, in principle, this allows for more efficient
          // reconfigurations for a fixed number of tasks, since that allows existing tasks to
          // only handle small modifications users to handle.
          .collect(Collectors.groupingBy(
              u -> Math.abs(u.hashCode()) % maxTasks,
              Collectors.joining(",")))
          .values().stream()
          .map(u -> {
            Map<String, String> taskConfig = new HashMap<>(baseConfig);
            taskConfig.put(HUAWEI_USERS_CONFIG, u);
            return taskConfig;
          })
          .collect(Collectors.toList());
      this.configuredUsers = SequencesKt.toSet(huaweiConfig.getUserRepository().stream());
      logger.info("Received userTask Configs {}", userTasks);
      return userTasks;
    } catch (Exception ex) {
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
