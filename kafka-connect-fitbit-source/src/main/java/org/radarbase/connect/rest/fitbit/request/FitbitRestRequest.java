/*
 * Copyright 2018 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.radarbase.connect.rest.fitbit.request;

import java.util.Map;
import java.util.function.Predicate;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.request.RequestRoute;
import org.radarbase.connect.rest.request.RestRequest;
import org.radarbase.convert.fitbit.DateRange;

/**
 * REST request taking into account the user and offsets queried. The offsets are useful for
 * defining what dates to poll (again).
 */
public class FitbitRestRequest extends RestRequest {
  private final User user;
  private final DateRange dateRange;

  public FitbitRestRequest(
      RequestRoute requestRoute, Request request, User user,
      Map<String, Object> partition, OkHttpClient client, DateRange dateRange,
      Predicate<RestRequest> isValid) {
    super(requestRoute, client, request, partition, isValid);
    this.user = user;
    this.dateRange = dateRange;
  }

  public User getUser() {
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
