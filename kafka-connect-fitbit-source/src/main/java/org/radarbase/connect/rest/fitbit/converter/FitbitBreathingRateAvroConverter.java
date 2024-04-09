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
import org.radarcns.connector.fitbit.FitbitBreathingRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.stream.Stream;

import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrNull;

public class FitbitBreathingRateAvroConverter extends FitbitAvroConverter {
    private static final Logger logger = LoggerFactory.getLogger(FitbitBreathingRateAvroConverter.class);
    private String breathingRateTopic;

    public FitbitBreathingRateAvroConverter(AvroData avroData) {
        super(avroData);
    }

    @Override
    public void initialize(RestSourceConnectorConfig config) {
        breathingRateTopic = ((FitbitRestSourceConnectorConfig) config).getFitbitBreathingRateTopic();
        logger.info("Using breathing rate topic {}", breathingRateTopic);
    }

    @Override
    protected Stream<TopicData> processRecords(FitbitRestRequest request, JsonNode root, double timeReceived) {
        JsonNode br = root.get("br");
        if (br == null || !br.isArray()) {
            logger.warn("No BR is provided for {}: {}", request, root);
            return Stream.empty();
        }
        ZonedDateTime startDate = request.getDateRange().end();

        return iterableToStream(br)
            .filter(m -> m != null && m.isObject())
            .map(tryOrNull(m -> parseBr(m, startDate, timeReceived),
                (a, ex) -> logger.warn("Failed to convert breathing rate from request {}, {}", request, a, ex)));
    }

    private TopicData parseBr(JsonNode data, ZonedDateTime startDate, double timeReceived) {
      Instant time = LocalDate.parse(data.get("dateTime").asText()).atStartOfDay(ZoneOffset.UTC).toInstant();
      JsonNode value = data.get("value");
      if (value == null || !value.isObject()) {
        return null;
      }
      FitbitBreathingRate fitbitBr = new FitbitBreathingRate(time.toEpochMilli() / 1000d,
              timeReceived,
              (float) value.get("deepSleepSummary").get("breathingRate").asDouble(),
              (float) value.get("remSleepSummary").get("breathingRate").asDouble(),
              (float) value.get("fullSleepSummary").get("breathingRate").asDouble(),
              (float) value.get("lightSleepSummary").get("breathingRate").asDouble());
      return new TopicData(time, breathingRateTopic, fitbitBr);
    }
}
