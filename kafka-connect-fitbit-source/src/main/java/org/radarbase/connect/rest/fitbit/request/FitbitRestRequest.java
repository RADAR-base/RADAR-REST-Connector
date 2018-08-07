package org.radarbase.connect.rest.fitbit.request;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.request.RequestRoute;
import org.radarbase.connect.rest.request.RestRequest;

import java.time.Instant;
import java.util.Map;

public class FitbitRestRequest extends RestRequest {
  private final FitbitUser user;
  private final Instant startOffset;
  private final Instant endOffset;

  public FitbitRestRequest(RequestRoute requestRoute, Request request, FitbitUser user, Map<String, Object> partition, OkHttpClient client, Instant startOffset, Instant endOffset) {
    super(requestRoute, request, partition, client);
    this.user = user;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }

  public FitbitUser getUser() {
    return user;
  }

  public Instant getStartOffset() {
    return startOffset;
  }

  public Instant getEndOffset() {
    return endOffset;
  }

  @Override
  public String toString() {
    return "FitbitRestRequest{"
        + "url=" + getRequest().url()
        + ", user=" + user
        + ", startOffset=" + startOffset
        + ", endOffset=" + endOffset
        + '}';
  }
}
