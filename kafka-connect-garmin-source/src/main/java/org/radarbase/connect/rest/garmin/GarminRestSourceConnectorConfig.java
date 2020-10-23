package org.radarbase.connect.rest.garmin;

import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

public class GarminRestSourceConnectorConfig extends RestSourceConnectorConfig {
  public GarminRestSourceConnectorConfig(ConfigDef config, Map<String, String> parsedConfig, boolean doLog) {
    super(config, parsedConfig, doLog);
  }

  public GarminRestSourceConnectorConfig(Map<String, String> parsedConfig, boolean doLog) {
    super(parsedConfig, doLog);
  }
}
