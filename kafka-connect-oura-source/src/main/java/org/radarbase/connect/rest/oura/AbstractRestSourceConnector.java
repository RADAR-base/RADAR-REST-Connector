package org.radarbase.connect.rest.oura;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.radarbase.connect.rest.oura.util.VersionUtil;

@SuppressWarnings("unused")
public abstract class AbstractRestSourceConnector extends SourceConnector {
  protected OuraRestSourceConnectorConfig config;

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public Class<? extends Task> taskClass() {
    return OuraSourceTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    return Collections.nCopies(maxTasks, new HashMap<>(config.originalsStrings()));
  }

  @Override
  public void start(Map<String, String> props) {
    config = getConfig(props);
  }

  public abstract OuraRestSourceConnectorConfig getConfig(Map<String, String> conf);

  @Override
  public void stop() {
  }
}
