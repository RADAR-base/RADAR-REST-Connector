package org.radarbase.connect.rest.request;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Map;

public class RestRequest {
  private final Request request;
  private final Map<String, Object> partition;
  private final RequestRoute route;
  private final OkHttpClient client;

  public RestRequest(RequestRoute route, Request request, Map<String, Object> partition,
      OkHttpClient client) {
    this.request = request;
    this.partition = partition;
    this.route = route;
    this.client = client;
  }

  public RestRequest(RestRequest request) {
    this(request.route, request.request, request.partition, request.client);
  }

  public Request getRequest() {
    return request;
  }

  public Map<String, Object> getPartition() {
    return partition;
  }

  public RequestRoute getRoute() {
    return route;
  }

  public OkHttpClient getClient() {
    return client;
  }

  public RestResponse withResponse(Response response) {
    return new RestResponse(this, response);
  }
}
