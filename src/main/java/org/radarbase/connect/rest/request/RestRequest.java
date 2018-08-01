package org.radarbase.connect.rest.request;

import okhttp3.Request;
import okhttp3.Response;

import java.util.Map;

public class RestRequest {
  private final Request request;
  private final Map<String, Object> partition;

  public RestRequest(Request request, Map<String, Object> partition) {
    this.request = request;
    this.partition = partition;
  }

  public RestRequest(RestRequest request) {
    this.request = request.request;
    this.partition = request.partition;
  }

  public Request getRequest() {
    return request;
  }

  public Map<String, Object> getPartition() {
    return partition;
  }

  public RestResponse withResponse(Response response) {
    return new RestResponse(this, response);
  }
}
