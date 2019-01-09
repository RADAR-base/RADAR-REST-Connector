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

package org.radarbase.connect.rest.request;

import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.nearFuture;

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
    return max(
        getLastPoll().plus(getPollInterval()),
        nextPolls()
            .min(Comparator.naturalOrder())
            .orElse(nearFuture()));
  }

  static <T extends Comparable<? super T>> T max(T a, T b) {
    return a != null && (b == null || a.compareTo(b) >= 0) ? a : b;
  }
}
