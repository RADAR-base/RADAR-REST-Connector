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
import org.radarbase.convert.fitbit.FitbitIntradayHeartRateDataConverter;

import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;

public class FitbitIntradayHeartRateRoute extends FitbitPollingRoute {
  public FitbitIntradayHeartRateRoute(FitbitRequestGenerator generator,
      UserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "heart_rate", avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/heart/date/%s/1d/1sec/time/%s/%s.json?timezone=UTC";
  }

  @Override
  protected FitbitDataConverter createConverter(FitbitRestSourceConnectorConfig config) {
    return new FitbitIntradayHeartRateDataConverter(config.getFitbitIntradayHeartRateTopic());
  }

  protected Stream<FitbitRestRequest> createRequests(User user) {
    return startDateGenerator(getOffset(user).plus(ONE_SECOND).truncatedTo(SECONDS))
        .map(dateRange -> newRequest(user, dateRange,
            user.getExternalUserId(), DATE_FORMAT.format(dateRange.getStart()),
            ISO_LOCAL_TIME.format(dateRange.getStart()),
            ISO_LOCAL_TIME.format(dateRange.getEnd().truncatedTo(SECONDS))));
  }
}
