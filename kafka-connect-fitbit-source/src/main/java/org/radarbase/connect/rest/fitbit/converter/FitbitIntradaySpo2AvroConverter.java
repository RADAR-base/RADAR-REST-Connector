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
import org.radarcns.connector.fitbit.FitbitIntradaySpo2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrNull;

public class FitbitIntradaySpo2AvroConverter extends FitbitAvroConverter {
    private static final Logger logger = LoggerFactory.getLogger(FitbitIntradaySpo2AvroConverter.class);
    private String spo2Topic;

    public FitbitIntradaySpo2AvroConverter(AvroData avroData) {
        super(avroData);
    }

    @Override
    public void initialize(RestSourceConnectorConfig config) {
        spo2Topic = ((FitbitRestSourceConnectorConfig) config).getFitbitIntradaySpo2Topic();
        logger.info("Using intraday spo2 topic {}", spo2Topic);
    }

    @Override
    protected Stream<TopicData> processRecords(FitbitRestRequest request, JsonNode root, double timeReceived) {
        JsonNode spo2 = root;
        if (spo2 == null || !spo2.isArray()) {
            logger.warn("No Spo2 is provided for {}: {}", request, root);
            return Stream.empty();
        }
        ZonedDateTime startDate = request.getDateRange().end();

        return iterableToStream(spo2)
                .filter(m -> m != null && m.isObject())
                .map(m -> m.get("minutes"))
                .filter(minutes -> minutes != null && minutes.isArray())
                .flatMap(FitbitAvroConverter::iterableToStream)
                .map(tryOrNull(minuteData -> parseSpo2(minuteData, startDate, timeReceived),
                        (a, ex) -> logger.warn("Failed to convert spo2 from request {}, {}", request, a, ex)));
    }

    private TopicData parseSpo2(JsonNode minuteData, ZonedDateTime startDate, double timeReceived) {
      Instant time = startDate.with(LocalDateTime.parse(minuteData.get("minute").asText())).toInstant();
      Float value = (float) minuteData.get("value").asDouble();
      if (value == null) {
        return null;
      }
      FitbitIntradaySpo2 fitbitSpo2 = new FitbitIntradaySpo2(time.toEpochMilli() / 1000d,
              timeReceived,
              (float) value);
      return new TopicData(time, spo2Topic, fitbitSpo2);
    }
}
