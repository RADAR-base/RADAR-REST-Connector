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

package org.radarbase.connect.rest.oura;

import static java.time.temporal.ChronoUnit.MILLIS;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.radarbase.connect.rest.oura.request.OuraReqGenerator;
import org.radarbase.oura.request.RestRequest;
import org.radarbase.connect.rest.util.VersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestSourceTask extends SourceTask {
  private static final Logger logger = LoggerFactory.getLogger(RestSourceTask.class);

  private OuraReqGenerator requestGenerator;

  @Override
  public void start(Map<String, String> map) {
    OuraRestSourceConnectorConfig connectorConfig;
    try {
      Class<?> connector = Class.forName(map.get("connector.class"));
      Object connectorInst = connector.getConstructor().newInstance();
      connectorConfig = ((OuraSourceConnector)connectorInst).getConfig(map);
    } catch (ClassNotFoundException e) {
      throw new ConnectException("Connector " + map.get("connector.class") + " not found", e);
    } catch (ReflectiveOperationException e) {
      throw new ConnectException("Connector " + map.get("connector.class")
          + " could not be instantiated", e);
    }
    requestGenerator = connectorConfig.getRequestGenerator(context.offsetStorageReader());
  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    long requestsGenerated = 0;
    List<SourceRecord> requests = Collections.emptyList();

    do {
      // FIX: Update timeout
      long timeout = 10000L;
      if (timeout > 0) {
        logger.info("Waiting {} milliseconds for next available request", timeout);
        Thread.sleep(timeout);
      }

      Map<String, String> configs = context.configs();
      Iterator<? extends RestRequest> requestIterator = requestGenerator.requests()
          .iterator();


      while (requests.isEmpty() && requestIterator.hasNext()) {
        RestRequest request = requestIterator.next();

        logger.info("Requesting {}", request.getRequest().url());
        requestsGenerated++;

        try {
          requests = this.requestGenerator.handleRequest(request)
              .collect(Collectors.toList());
        } catch (IOException ex) {
          logger.warn("Failed to make request: {}", ex.toString());
        }
      }
    } while (requests.isEmpty());

    logger.info("Processed {} records from {} URLs", requests.size(), requestsGenerated);

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
