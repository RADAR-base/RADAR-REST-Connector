package org.radarbase.connect.rest.selector;

import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.request.RestRequest;

public class SimpleTopicSelector implements TopicSelector {
  private String topic;

  @Override
  public String getTopic(RestRequest request, Object result) {
    return topic;
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    // Always return the first topic in the list
    topic = config.getTopics().get(0);
  }
}
