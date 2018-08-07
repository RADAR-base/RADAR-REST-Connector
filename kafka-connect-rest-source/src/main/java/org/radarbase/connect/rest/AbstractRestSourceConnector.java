package org.radarbase.connect.rest;

import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.radarbase.connect.rest.util.VersionUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class AbstractRestSourceConnector extends SourceConnector {
  protected RestSourceConnectorConfig config;

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public Class<? extends Task> taskClass() {
    return RestSourceTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    return Collections.nCopies(maxTasks, new HashMap<>(config.originalsStrings()));
  }

  @Override
  public void start(Map<String, String> props) {
    config = getConfig(props);
  }

  public abstract RestSourceConnectorConfig getConfig(Map<String, String> conf);

  @Override
  public void stop() {
  }
}
