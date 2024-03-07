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

import io.confluent.connect.avro.AvroData;
import org.radarbase.connect.rest.fitbit.converter.FitbitIntradaySpo2AvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;

import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.SECONDS;
import java.time.Duration;


public class FitbitIntradaySpo2Route extends FitbitPollingRoute {
  private final FitbitIntradaySpo2AvroConverter converter;

  public FitbitIntradaySpo2Route(FitbitRequestGenerator generator,
      UserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "intraday_spo2");
    this.converter = new FitbitIntradaySpo2AvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/spo2/date/%s/%s/all.json";
  }

  protected Stream<FitbitRestRequest> createRequests(User user) {
    return startDateGenerator(getOffset(user).plus(ONE_SECOND).truncatedTo(SECONDS))
        .map(dateRange -> newRequest(user, dateRange,
            user.getExternalUserId(), DATE_FORMAT.format(dateRange.start()), DATE_FORMAT.format(dateRange.end())));
  }

  /** Limit range to 30 days as documented here: https://dev.fitbit.com/build/reference/web-api/intraday/get-spo2-intraday-by-interval/ */
  @Override
  Duration getDateRangeInterval() {
    return THIRTY_DAYS;
  }

  @Override
  public FitbitIntradaySpo2AvroConverter converter() {
    return converter;
  }
}