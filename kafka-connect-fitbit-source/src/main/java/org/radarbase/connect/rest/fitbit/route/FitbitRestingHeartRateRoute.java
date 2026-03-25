/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.connect.rest.fitbit.route;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;

import io.confluent.connect.avro.AvroData;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.radarbase.connect.rest.fitbit.converter.FitbitRestingHeartRateAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.connect.rest.fitbit.util.DateRange;

public class FitbitRestingHeartRateRoute extends FitbitPollingRoute {
  private static final Duration RESTING_HEART_RATE_POLL_INTERVAL = Duration.ofDays(1);

  private final FitbitRestingHeartRateAvroConverter converter;

  public FitbitRestingHeartRateRoute(
      FitbitRequestGenerator generator, UserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "resting_heart_rate");
    this.converter = new FitbitRestingHeartRateAvroConverter(avroData);
  }

  @Override
  protected Stream<FitbitRestRequest> createRequests(User user) {
    // Important: resting heart rate is queried at the resolution of a single
    // day, so the offset for the next request will be set to the next day.
    ZonedDateTime startDate = this.getOffset(user).plus(ONE_DAY)
        .atZone(UTC)
        .truncatedTo(DAYS);
    // Note: the date range of startDate to now() is not correct, but will ensure that in case of empty
    // results, the HISTORICAL_TIME_DAYS retry inactivation in requestEmpty() of FitbitPollingRoute.java
    // will never be used.
    return Stream.of(newRequest(user, new DateRange(startDate, ZonedDateTime.now(UTC)),
        user.getExternalUserId(), DATE_FORMAT.format(startDate)));
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/heart/date/%s/1d.json?timezone=UTC";
  }

  @Override
  protected Duration getPollIntervalPerUser() {
    return RESTING_HEART_RATE_POLL_INTERVAL;
  }

  @Override
  public FitbitRestingHeartRateAvroConverter converter() {
    return converter;
  }
}
