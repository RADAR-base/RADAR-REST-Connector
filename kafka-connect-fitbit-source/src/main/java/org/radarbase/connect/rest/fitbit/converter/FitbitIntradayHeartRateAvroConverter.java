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

package org.radarbase.connect.rest.fitbit.converter;

import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.connect.avro.AvroData;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarcns.connector.fitbit.FitbitIntradayHeartRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitIntradayHeartRateAvroConverter extends FitbitAvroConverter {
  private static final Logger logger = LoggerFactory.getLogger(
      FitbitIntradayHeartRateAvroConverter.class);
  private String heartRateTopic;

  public FitbitIntradayHeartRateAvroConverter(AvroData avroData) {
    super(avroData);
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    heartRateTopic = ((FitbitRestSourceConnectorConfig) config).getFitbitIntradayHeartRateTopic();
    logger.info("Using heart rate topic {}", heartRateTopic);
  }

  @Override
  protected Stream<TopicData> processRecords(
      FitbitRestRequest request, JsonNode root, double timeReceived) {
    JsonNode intraday = root.get("activities-heart-intraday");
    if (intraday == null || !intraday.isObject()) {
      return Stream.empty();
    }

    JsonNode dataset = intraday.get("dataset");
    if (dataset == null || !dataset.isArray()) {
      return Stream.empty();
    }

    int interval = getRecordInterval(intraday, 1);

    // Used as the date to convert the local times in the dataset to absolute times.
    ZonedDateTime startDate = request.getDateRange().start();

    return iterableToStream(dataset)
        .map(tryOrNull(activity -> {
          Instant time = startDate.with(LocalTime.parse(activity.get("time").asText()))
              .toInstant();

          FitbitIntradayHeartRate heartRate = new FitbitIntradayHeartRate(
              time.toEpochMilli() / 1000d,
              timeReceived,
              interval,
              activity.get("value").asInt());

          return new TopicData(time, heartRateTopic, heartRate);
        }, (a, ex) -> logger.warn(
            "Failed to convert heart rate from request {} of user {}, {}",
            request.getRequest().url(), request.getUser(), a, ex)));
  }
}
