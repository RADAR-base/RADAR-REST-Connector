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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.radarbase.connect.rest.fitbit.route.FitbitPollingRoute.DATE_FORMAT;
import static org.radarbase.connect.rest.fitbit.route.FitbitPollingRoute.TIME_FORMAT;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class FitbitPollingRouteTest {

  @Test
  public void testInstant() {
    ZonedDateTime date = Instant.parse("2018-08-08T00:11:22.333Z")
        .atZone(ZoneOffset.UTC)
        .truncatedTo(ChronoUnit.MINUTES);

    assertEquals("2018-08-08", DATE_FORMAT.format(date));
    assertEquals("00:11", TIME_FORMAT.format(date));
  }
}
