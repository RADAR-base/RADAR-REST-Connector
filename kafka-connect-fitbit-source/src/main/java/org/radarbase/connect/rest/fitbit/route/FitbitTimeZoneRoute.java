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
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.radarbase.connect.rest.fitbit.converter.FitbitTimeZoneAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;
import org.radarbase.connect.rest.fitbit.util.DateRange;

public class FitbitTimeZoneRoute extends FitbitPollingRoute {
  protected static final Duration TIME_ZONE_POLL_INTERVAL = Duration.ofHours(1);

  private final FitbitTimeZoneAvroConverter converter;

  public FitbitTimeZoneRoute(FitbitRequestGenerator generator,
      FitbitUserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "timezone");
    this.converter = new FitbitTimeZoneAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/profile.json";
  }

  protected Stream<FitbitRestRequest> createRequests(FitbitUser user) {
    ZonedDateTime now = ZonedDateTime.now(UTC);
    return Stream.of(newRequest(user, new DateRange(now, now), user.getFitbitUserId()));
  }

  @Override
  public FitbitTimeZoneAvroConverter converter() {
    return converter;
  }

  @Override
  protected Duration getPollIntervalPerUser() {
    return TIME_ZONE_POLL_INTERVAL;
  }

  @Override
  protected Duration getLookbackTime() {
    return Duration.ZERO;
  }
}
