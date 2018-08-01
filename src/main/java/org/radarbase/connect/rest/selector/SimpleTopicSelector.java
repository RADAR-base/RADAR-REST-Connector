package org.radarbase.connect.rest.selector;

import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.request.RestResponse;

public class SimpleTopicSelector implements TopicSelector {
  private String topic;

  @Override
  public String getTopic(RestResponse response) {
    return topic;
  }

  @Override
  public void start(RestSourceConnectorConfig config) {
    // Always return the first topic in the list
    topic = config.getTopics().get(0);
  }
}
