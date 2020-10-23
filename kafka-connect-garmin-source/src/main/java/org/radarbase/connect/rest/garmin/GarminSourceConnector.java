package org.radarbase.connect.rest.garmin;

import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.radarbase.connect.rest.AbstractRestSourceConnector;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

public class GarminSourceConnector extends AbstractRestSourceConnector {
  @Override
  public RestSourceConnectorConfig getConfig(Map<String, String> conf) {
    return null;
  }

  @Override
  public ConfigDef config() {
    return null;
  }
}
