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

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.connect.avro.AvroData;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarcns.connector.fitbit.FitbitIntradayHeartRateVariability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrNull;

public class FitbitIntradayHeartRateVariabilityAvroConverter extends FitbitAvroConverter {
    private static final Logger logger = LoggerFactory.getLogger(FitbitIntradayHeartRateVariabilityAvroConverter.class);
    private String heartRateVariabilityTopic;

    public FitbitIntradayHeartRateVariabilityAvroConverter(AvroData avroData) {
        super(avroData);
    }

    @Override
    public void initialize(RestSourceConnectorConfig config) {
        heartRateVariabilityTopic = ((FitbitRestSourceConnectorConfig) config).getFitbitIntradayHeartRateVariabilityTopic();
        logger.info("Using intraday heart rate variability topic {}", heartRateVariabilityTopic);
    }

    @Override
    protected Stream<TopicData> processRecords(FitbitRestRequest request, JsonNode root, double timeReceived) {
        JsonNode hrv = root.get("hrv");
        if (hrv == null || !hrv.isArray()) {
            logger.warn("No HRV is provided for {}: {}", request, root);
            return Stream.empty();
        }
        ZonedDateTime startDate = request.getDateRange().end();

        return iterableToStream(hrv)
                .filter(m -> m != null && m.isObject())
                .map(m -> m.get("minutes"))
                .filter(minutes -> minutes != null && minutes.isArray())
                .flatMap(FitbitAvroConverter::iterableToStream)
                .map(tryOrNull(minuteData -> parseHrv(minuteData, startDate, timeReceived),
                        (a, ex) -> logger.warn("Failed to convert heart rate variability from request {}, {}", request, a, ex)));
    }

    private TopicData parseHrv(JsonNode minuteData, ZonedDateTime startDate, double timeReceived) {
      Instant time = startDate.with(LocalDateTime.parse(minuteData.get("minute").asText())).toInstant();
      JsonNode value = minuteData.get("value");
      if (value == null || !value.isObject()) {
        return null;
      }
      FitbitIntradayHeartRateVariability fitbitHrv = new FitbitIntradayHeartRateVariability(time.toEpochMilli() / 1000d,
              timeReceived,
              (float) value.get("rmssd").asDouble(),
              (float) value.get("coverage").asDouble(),
              (float) value.get("hf").asDouble(),
              (float) value.get("lf").asDouble());
      return new TopicData(time, heartRateVariabilityTopic, fitbitHrv);
    }
}
