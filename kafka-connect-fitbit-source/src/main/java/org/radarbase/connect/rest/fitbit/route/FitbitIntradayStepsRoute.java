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

import static java.time.temporal.ChronoUnit.MINUTES;

import io.confluent.connect.avro.AvroData;
import java.time.temporal.TemporalAmount;
import java.util.stream.Stream;
import org.radarbase.connect.rest.fitbit.converter.FitbitIntradayStepsAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;

public class FitbitIntradayStepsRoute extends FitbitPollingRoute {
  private static final TemporalAmount ONE_MINUTE = MINUTES.getDuration();

  private final FitbitIntradayStepsAvroConverter converter;

  public FitbitIntradayStepsRoute(FitbitRequestGenerator generator,
      UserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "intraday_steps");
    this.converter = new FitbitIntradayStepsAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/steps/date/%s/1d/1min/time/%s/%s.json?timezone=UTC";
  }

  protected Stream<FitbitRestRequest> createRequests(User user) {
    return startDateGenerator(this.getOffset(user).plus(ONE_MINUTE).truncatedTo(MINUTES))
        .map(dateRange -> newRequest(user, dateRange,
            user.getExternalUserId(), DATE_FORMAT.format(dateRange.start()),
            TIME_FORMAT.format(dateRange.start()), TIME_FORMAT.format(dateRange.end())));
  }

  @Override
  public FitbitIntradayStepsAvroConverter converter() {
    return converter;
  }
}
