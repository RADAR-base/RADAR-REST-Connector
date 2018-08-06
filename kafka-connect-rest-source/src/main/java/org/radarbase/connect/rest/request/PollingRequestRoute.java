package org.radarbase.connect.rest.request;

import java.util.stream.LongStream;

public interface PollingRequestRoute extends RequestRoute {
  long getPollInterval();
  LongStream nextPolls();

  default long getTimeOfNextRequest() {
    return getPollInterval() + nextPolls()
        .min()
        .orElse(Long.MAX_VALUE - getPollInterval());
  }
}
