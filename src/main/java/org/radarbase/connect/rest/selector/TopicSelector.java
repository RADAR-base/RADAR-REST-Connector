package org.radarbase.connect.rest.selector;

import org.radarbase.connect.rest.RestSourceConnectorConfig;

public interface TopicSelector {
  String getTopic(Object data);

  void start(RestSourceConnectorConfig config);
}
