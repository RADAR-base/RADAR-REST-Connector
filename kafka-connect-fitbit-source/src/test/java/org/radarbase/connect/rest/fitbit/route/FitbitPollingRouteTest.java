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
