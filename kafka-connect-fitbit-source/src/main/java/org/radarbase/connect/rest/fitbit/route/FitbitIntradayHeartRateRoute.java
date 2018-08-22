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

package org.radarbase.connect.rest.fitbit.route;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;

import io.confluent.connect.avro.AvroData;
import java.util.stream.Stream;
import org.radarbase.connect.rest.fitbit.converter.FitbitIntradayHeartRateAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;

public class FitbitIntradayHeartRateRoute extends FitbitPollingRoute {
  private static final String ROUTE_NAME = "heart_rate";
  private final FitbitIntradayHeartRateAvroConverter converter;

  public FitbitIntradayHeartRateRoute(FitbitRequestGenerator generator,
      FitbitUserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, ROUTE_NAME);
    this.converter = new FitbitIntradayHeartRateAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/heart/date/%s/1d/1sec/time/%s/%s.json?timezone=UTC";
  }

  protected Stream<FitbitRestRequest> createRequests(FitbitUser user) {
    return startDateGenerator(getOffset(user).plus(ONE_SECOND).truncatedTo(SECONDS))
        .map(dateRange -> newRequest(user, dateRange,
            user.getFitbitUserId(), DATE_FORMAT.format(dateRange.start()),
            ISO_LOCAL_TIME.format(dateRange.start()),
            ISO_LOCAL_TIME.format(dateRange.end().truncatedTo(SECONDS))));
  }

  @Override
  public FitbitIntradayHeartRateAvroConverter converter() {
    return converter;
  }
}
