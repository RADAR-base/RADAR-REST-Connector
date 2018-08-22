package org.radarbase.connect.rest.fitbit.util;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;

public class DateRange {
  private final ZonedDateTime from;
  private final ZonedDateTime to;

  public DateRange(ZonedDateTime from, ZonedDateTime to) {
    this.from = from;
    this.to = to;
  }

  public DateRange(ZonedDateTime from, TemporalAmount duration) {
    this.from = from;
    this.to = from.plus(duration);
  }


  public ZonedDateTime from() {
    return from;
  }

  public ZonedDateTime to() {
    return to;
  }
}
