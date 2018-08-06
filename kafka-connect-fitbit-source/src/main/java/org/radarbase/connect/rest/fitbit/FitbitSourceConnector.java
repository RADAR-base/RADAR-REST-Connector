package org.radarbase.connect.rest.fitbit;

import org.apache.kafka.common.config.ConfigDef;
import org.radarbase.connect.rest.AbstractRestSourceConnector;

import java.util.Map;

public class FitbitSourceConnector extends AbstractRestSourceConnector {
  @Override
  public FitbitRestSourceConnectorConfig getConfig(Map<String, String> conf) {
    return new FitbitRestSourceConnectorConfig(conf);
  }

  @Override
  public ConfigDef config() {
    return FitbitRestSourceConnectorConfig.conf();
  }
}
