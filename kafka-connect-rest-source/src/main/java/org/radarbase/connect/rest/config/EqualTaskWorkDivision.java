package org.radarbase.connect.rest.config;

import org.radarbase.connect.rest.RestSourceConnectorConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EqualTaskWorkDivision implements TaskWorkDivision {
  private RestSourceConnectorConfig config;

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    return Collections.nCopies(maxTasks, new HashMap<>(config.originalsStrings()));
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    this.config = config;
  }
}
