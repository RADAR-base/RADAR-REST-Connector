package org.radarbase.connect.rest.adapter;

import org.apache.kafka.common.config.ConfigDef;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

public interface RestAdapter {
  void embedConfig(ConfigDef configDef);
  RestSourceConnectorConfig getConfig();

}
