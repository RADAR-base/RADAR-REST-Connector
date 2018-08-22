package org.radarbase.connect.rest.fitbit.request;

import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.util.DateRange;
import org.radarbase.connect.rest.request.RequestRoute;
import org.radarbase.connect.rest.request.RestRequest;

/**
 * REST request taking into account the user and offsets queried. The offsets are useful for
 * defining what dates to poll (again).
 */
public class FitbitRestRequest extends RestRequest {
  private final FitbitUser user;
  private final DateRange dateRange;

  public FitbitRestRequest(
      RequestRoute requestRoute, Request request, FitbitUser user,
      Map<String, Object> partition, OkHttpClient client, DateRange dateRange) {
    super(requestRoute, client, request, partition);
    this.user = user;
    this.dateRange = dateRange;
  }

  public FitbitUser getUser() {
    return user;
  }

  public DateRange getDateRange() {
    return dateRange;
  }

  @Override
  public String toString() {
    return "FitbitRestRequest{"
        + "url=" + getRequest().url()
        + ", user=" + user
        + ", dateRange=" + dateRange
        + '}';
  }
}
