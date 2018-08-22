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
import java.util.Comparator;
import java.util.stream.Stream;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

public abstract class RequestGeneratorRouter implements RequestGenerator {
  @Override
  public Stream<RestRequest> requests() {
    return routes()
        .flatMap(RequestRoute::requests);
  }

  @Override
  public Instant getTimeOfNextRequest() {
    return routes()
        .map(RequestRoute::getTimeOfNextRequest)
        .min(Comparator.naturalOrder())
        .orElse(Instant.MAX);
  }

  public abstract Stream<RequestRoute> routes();

  public void setOffsetStorageReader(OffsetStorageReader offsetStorageReader) {
    routes().forEach(r -> r.setOffsetStorageReader(offsetStorageReader));
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    routes().forEach(r -> r.initialize(config));
  }
}
