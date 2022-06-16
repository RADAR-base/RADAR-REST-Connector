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
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.convert.fitbit.FitbitDataConverter;
import org.radarbase.convert.fitbit.FitbitIntradayStepsDataConverter;

import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.MINUTES;

public class FitbitIntradayStepsRoute extends FitbitPollingRoute {
  public FitbitIntradayStepsRoute(FitbitRequestGenerator generator,
      UserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "intraday_steps", avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/steps/date/%s/1d/1min/time/%s/%s.json?timezone=UTC";
  }

  @Override
  protected FitbitDataConverter createConverter(FitbitRestSourceConnectorConfig config) {
    return new FitbitIntradayStepsDataConverter(config.getFitbitIntradayStepsTopic());
  }

  protected Stream<FitbitRestRequest> createRequests(User user) {
    return startDateGenerator(this.getOffset(user).plus(ONE_MINUTE).truncatedTo(MINUTES))
        .map(dateRange -> newRequest(user, dateRange,
            user.getExternalUserId(), DATE_FORMAT.format(dateRange.getStart()),
            TIME_FORMAT.format(dateRange.getStart()), TIME_FORMAT.format(dateRange.getEnd())));
  }
}
