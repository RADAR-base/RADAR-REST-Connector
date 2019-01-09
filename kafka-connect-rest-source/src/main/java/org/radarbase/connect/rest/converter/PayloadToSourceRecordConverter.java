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

package org.radarbase.connect.rest.converter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import okhttp3.Response;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.config.RestSourceTool;
import org.radarbase.connect.rest.request.RestRequest;

public interface PayloadToSourceRecordConverter extends RestSourceTool {
  Instant MIN_INSTANT = Instant.EPOCH;
  String TIMESTAMP_OFFSET_KEY = "timestamp";
  TemporalAmount NEAR_FUTURE = Duration.ofDays(31L);

  Collection<SourceRecord> convert(
      RestRequest request, Response response) throws IOException;

  static Instant nearFuture() {
    return Instant.now().plus(NEAR_FUTURE);
  }
}
