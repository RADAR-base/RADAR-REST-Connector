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

package org.radarbase.connect.rest;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.radarbase.connect.rest.request.RequestGenerator;
import org.radarbase.connect.rest.request.RestRequest;
import org.radarbase.connect.rest.util.VersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestSourceTask extends SourceTask {
  private static Logger logger = LoggerFactory.getLogger(RestSourceTask.class);

  private RequestGenerator requestGenerator;

  @Override
  public void start(Map<String, String> map) {
    RestSourceConnectorConfig connectorConfig;
    try {
      Class<?> connector = Class.forName(map.get("connector.class"));
      connectorConfig = ((AbstractRestSourceConnector)connector.newInstance()).getConfig(map);
    } catch (ClassNotFoundException e) {
      throw new ConnectException("Connector " + map.get("connector.class") + " not found", e);
    } catch (IllegalAccessException | InstantiationException e) {
      throw new ConnectException("Connector " + map.get("connector.class")
          + " could not be instantiated", e);
    }
    requestGenerator = connectorConfig.getRequestGenerator();
    requestGenerator.setOffsetStorageReader(context.offsetStorageReader());
  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    long timeout = MILLIS.between(Instant.now(), requestGenerator.getTimeOfNextRequest());
    if (timeout > 0) {
      logger.info("Waiting {} milliseconds for next available request", timeout);
      Thread.sleep(timeout);
    }

    LongAdder requestsGenerated = new LongAdder();

    List<SourceRecord> requests = requestGenerator.requests()
        .peek(r -> {
          logger.info("Requesting {}", r.getRequest().url());
          requestsGenerated.increment();
        })
        .flatMap(tryOrNull(RestRequest::handleRequest,
            (r, ex) -> logger.warn("Failed to make request: {}", ex.toString())))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    logger.info("Processed {} records from {} URLs", requests.size(), requestsGenerated.sum());

    return requests;
  }

  @Override
  public void stop() {
    logger.debug("Stopping source task");
  }

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }
}
