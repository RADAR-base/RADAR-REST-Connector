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

import static java.time.ZoneOffset.UTC;

import io.confluent.connect.avro.AvroData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import org.radarbase.connect.rest.fitbit.converter.FitbitIntradayHeartRateAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.connect.rest.fitbit.util.DateRange;

public class FitbitIntradayHeartRateRoute extends FitbitPollingRoute {
  private final FitbitIntradayHeartRateAvroConverter converter;

  public FitbitIntradayHeartRateRoute(FitbitRequestGenerator generator,
      UserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "heart_rate");
    this.converter = new FitbitIntradayHeartRateAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    // URL format args: user-id, date
    return baseUrl + "/1/user/%s/activities/heart/date/%s/1d/1sec.json?timezone=UTC";
  }

  protected Stream<FitbitRestRequest> createRequests(User user) {
    // Important: heart rate is queried at the resolution of a single
    // day, so the offset for the next request will be set to the next day.
    Instant startDate = this.getOffset(user).plus(ONE_DAY)
        .atZone(UTC)
        .truncatedTo(ChronoUnit.DAYS).toInstant();
    List<DateRange> dateRangeStream = startDateGenerator(startDate).toList();
    return dateRangeStream.stream()
        .map(dateRange -> newRequest(user, dateRange,
            user.getExternalUserId(),
            DATE_FORMAT.format(dateRange.start())
        ));
  }

  @Override
  public FitbitIntradayHeartRateAvroConverter converter() {
    return converter;
  }
}
