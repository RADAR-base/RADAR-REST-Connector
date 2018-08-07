package org.radarbase.connect.rest.request;

import java.util.stream.LongStream;

public interface PollingRequestRoute extends RequestRoute {
  long getPollInterval();
  long getLastPoll();
  LongStream nextPolls();

  default long getTimeOfNextRequest() {
    return Math.max(getLastPoll() + getPollInterval(),
        nextPolls().min().orElse(Long.MAX_VALUE));
  }
}
