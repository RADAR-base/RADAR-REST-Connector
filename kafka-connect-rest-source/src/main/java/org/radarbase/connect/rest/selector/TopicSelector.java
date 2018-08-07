package org.radarbase.connect.rest.selector;

import org.radarbase.connect.rest.config.RestSourceTool;
import org.radarbase.connect.rest.request.RestRequest;

public interface TopicSelector extends RestSourceTool {
  String getTopic(RestRequest request, Object result);
}
