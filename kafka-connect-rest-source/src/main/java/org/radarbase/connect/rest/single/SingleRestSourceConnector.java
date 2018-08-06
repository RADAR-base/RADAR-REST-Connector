package org.radarbase.connect.rest.single;

import org.apache.kafka.common.config.ConfigDef;
import org.radarbase.connect.rest.AbstractRestSourceConnector;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

import java.util.Map;

public class SingleRestSourceConnector extends AbstractRestSourceConnector {
  @Override
  public ConfigDef config() {
    return SingleRestSourceConnectorConfig.conf();
  }

  @Override
  public RestSourceConnectorConfig getConfig(Map<String, String> conf) {
    return new SingleRestSourceConnectorConfig(conf);
  }
}
