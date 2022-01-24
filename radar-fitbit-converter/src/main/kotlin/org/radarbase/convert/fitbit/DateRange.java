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

package org.radarbase.connect.rest.fitbit.util;

import java.time.ZonedDateTime;
import java.util.Objects;

public class DateRange {
  private final ZonedDateTime start;
  private final ZonedDateTime end;

  public DateRange(ZonedDateTime start, ZonedDateTime end) {
    this.start = start;
    this.end = end;
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
