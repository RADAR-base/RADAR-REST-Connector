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

import java.time.Instant;
import java.util.stream.Stream;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.config.RestSourceTool;

/**
 * Dynamically generates requests. The requests should be based on the offsets that are stored in
 * the response SourceRecord.
 */
public interface RequestGenerator extends RestSourceTool {
  Instant getTimeOfNextRequest();

  /**
   * Requests that should be queried next.
   */
  Stream<? extends RestRequest> requests();

  /**
   * Set the Kafka offset storage reader. This allows for resetting the request intervals.
   * @param offsetStorageReader possibly null offset storage reader.
   */
  void setOffsetStorageReader(OffsetStorageReader offsetStorageReader);
}
