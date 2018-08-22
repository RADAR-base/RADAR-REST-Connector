package org.radarbase.connect.rest.single;

import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.radarbase.connect.rest.AbstractRestSourceConnector;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

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
