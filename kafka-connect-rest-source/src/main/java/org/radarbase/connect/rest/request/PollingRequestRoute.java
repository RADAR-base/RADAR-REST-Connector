package org.radarbase.connect.rest.request;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

public interface PollingRequestRoute extends RequestRoute {
  /** General polling interval for retrying this route. */
  Duration getPollInterval();
  /** Last time the route was polled. */
  Instant getLastPoll();
  /** Actual times that new data will be needed. */
  Stream<Instant> nextPolls();

  /** Get the time that this route should be polled again. */
  default Instant getTimeOfNextRequest() {
    return max(getLastPoll().plus(getPollInterval()),
        nextPolls().min(Comparator.naturalOrder()).orElse(Instant.MAX));
  }

  static <T extends Comparable<? super T>> T max(T a, T b) {
    return a != null && (b == null || a.compareTo(b) >= 0) ? a : b;
  }
}
