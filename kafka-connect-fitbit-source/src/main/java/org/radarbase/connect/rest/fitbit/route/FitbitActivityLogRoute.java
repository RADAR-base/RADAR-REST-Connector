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
import static java.time.temporal.ChronoUnit.SECONDS;

import io.confluent.connect.avro.AvroData;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import org.radarbase.connect.rest.fitbit.converter.FitbitActivityLogAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.connect.rest.fitbit.util.DateRange;

public class FitbitActivityLogRoute extends FitbitPollingRoute {
  public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
      .withZone(UTC);
  private static final Duration ACTIVITY_LOG_POLL_INTERVAL = Duration.ofDays(1);

  private final FitbitActivityLogAvroConverter converter;

  public FitbitActivityLogRoute(FitbitRequestGenerator generator, UserRepository userRepository,
      AvroData avroData) {
    super(generator, userRepository, "activity_log");
    converter = new FitbitActivityLogAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/list.json?sort=asc&afterDate=%s&limit=20&offset=0";
  }

  /**
   * Actually construct a request, based on the current offset
   * @param user Fitbit user
   * @return request to make
   */
  protected Stream<FitbitRestRequest> createRequests(User user) {
    ZonedDateTime startDate = this.getOffset(user).plus(ONE_SECOND)
        .atZone(UTC)
        .truncatedTo(SECONDS);

    return Stream.of(newRequest(user, new DateRange(startDate, ZonedDateTime.now(UTC)),
        user.getExternalUserId(), DATE_TIME_FORMAT.format(startDate)));
  }


  @Override
  protected Duration getPollIntervalPerUser() {
    return ACTIVITY_LOG_POLL_INTERVAL;
  }

  @Override
  public FitbitActivityLogAvroConverter converter() {
    return converter;
  }
}
