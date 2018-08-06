package org.radarbase.connect.rest.fitbit.request;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.radarbase.connect.rest.fitbit.FitbitUser;
import org.radarbase.connect.rest.request.RequestRoute;
import org.radarbase.connect.rest.request.RestRequest;

import java.util.Map;

public class FitbitRestRequest extends RestRequest {
  private final FitbitUser user;

  public FitbitRestRequest(RequestRoute requestRoute, Request request, FitbitUser user, Map<String, Object> partition, OkHttpClient client) {
    super(requestRoute, request, partition, client);
    this.user = user;
  }

  public FitbitRestRequest(FitbitRestRequest request) {
    super(request);
    this.user = request.user;
  }

  public FitbitUser getUser() {
    return user;
  }
}
