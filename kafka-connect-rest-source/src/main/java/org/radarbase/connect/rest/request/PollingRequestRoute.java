package org.radarbase.connect.rest.request;

import java.util.stream.LongStream;

public interface PollingRequestRoute extends RequestRoute {
  long getPollInterval();
  LongStream nextPolls();

  default long getMaxPollInterval() {
    return Long.MAX_VALUE - getPollInterval();
  }
  default long getTimeOfNextRequest() {
    return getPollInterval() + nextPolls()
        .min()
        .orElse(getMaxPollInterval());
  }
}
