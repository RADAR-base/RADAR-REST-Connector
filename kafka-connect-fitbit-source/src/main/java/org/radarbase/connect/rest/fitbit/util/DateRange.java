package org.radarbase.connect.rest.fitbit.util;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Objects;

public class DateRange {
  private final ZonedDateTime start;
  private final ZonedDateTime end;

  public DateRange(ZonedDateTime start, ZonedDateTime end) {
    this.start = start;
    this.end = end;
  }

  public DateRange(ZonedDateTime start, TemporalAmount duration) {
    this.start = start;
    this.end = start.plus(duration);
  }

  public ZonedDateTime start() {
    return start;
  }

  public ZonedDateTime end() {
    return end;
  }

  @Override
  public String toString() {
    return "DateRange{"
        + "start=" + start
        + ", end=" + end
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DateRange dateRange = (DateRange) o;
    return Objects.equals(start, dateRange.start) &&
        Objects.equals(end, dateRange.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }
}
