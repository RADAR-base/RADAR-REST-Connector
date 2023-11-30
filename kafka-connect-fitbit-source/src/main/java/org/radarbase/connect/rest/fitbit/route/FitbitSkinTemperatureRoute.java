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
import org.radarbase.connect.rest.fitbit.converter.FitbitSkinTemperatureAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.connect.rest.fitbit.util.DateRange;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;

public class FitbitSkinTemperatureRoute extends FitbitPollingRoute {
  private final FitbitSkinTemperatureAvroConverter converter;

  public FitbitSkinTemperatureRoute(FitbitRequestGenerator generator,
                                    UserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "skin_temperature");
    this.converter = new FitbitSkinTemperatureAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/temp/skin/date/%s/%s/.json";
  }

  protected Stream<FitbitRestRequest> createRequests(User user) {
    ZonedDateTime startDate = this.getOffset(user).plus(ONE_SECOND)
            .atZone(UTC)
            .truncatedTo(SECONDS);
    ZonedDateTime now = ZonedDateTime.now(UTC);
    return Stream.of(newRequest(user, new DateRange(startDate, now),
            user.getExternalUserId(), DATE_FORMAT.format(startDate), DATE_FORMAT.format(now)));
  }

  @Override
  public FitbitSkinTemperatureAvroConverter converter() {
    return converter;
  }
}
