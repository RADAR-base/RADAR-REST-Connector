package org.radarbase.connect.rest.config;

import org.radarbase.connect.rest.RestSourceConnectorConfig;

public interface RestSourceTool {
  void start(RestSourceConnectorConfig config);
}
