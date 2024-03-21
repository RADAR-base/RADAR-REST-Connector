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
import org.radarcns.connector.fitbit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrNull;

public class FitbitSkinTemperatureAvroConverter extends FitbitAvroConverter {
  private static final Logger logger = LoggerFactory.getLogger(FitbitSkinTemperatureAvroConverter.class);

  private static final Map<String, FitbitSkinTemperatureLogType> LOG_TYPE_MAP = new HashMap<>();

  static {
    LOG_TYPE_MAP.put("dedicated_temp_sensor", FitbitSkinTemperatureLogType.DEDICATED_TEMP_SENSOR);
    LOG_TYPE_MAP.put("other_sensors", FitbitSkinTemperatureLogType.OTHER_SENSORS);
  }

  private String skinTemperatureTopic;

    public FitbitSkinTemperatureAvroConverter(AvroData avroData) {
        super(avroData);
    }

    @Override
    public void initialize(RestSourceConnectorConfig config) {
      skinTemperatureTopic = ((FitbitRestSourceConnectorConfig) config).getFitbitSkinTemperatureTopic();
        logger.info("Using skin temperature topic {}", skinTemperatureTopic);
    }

    @Override
    protected Stream<TopicData> processRecords(FitbitRestRequest request, JsonNode root, double timeReceived) {
        JsonNode tempSkin = root.get("tempSkin");
        if (tempSkin == null || !tempSkin.isArray()) {
            logger.warn("No tempSkin is provided for {}: {}", request, root);
            return Stream.empty();
        }
        ZonedDateTime startDate = request.getDateRange().end();

        return iterableToStream(tempSkin)
                .filter(m -> m != null && m.isObject())
                .map(tryOrNull(m -> parseTempSkin(m, startDate, timeReceived),
                        (a, ex) -> logger.warn("Failed to convert skin temperature from request {}, {}", request, a, ex)));
    }

    private TopicData parseTempSkin(JsonNode data, ZonedDateTime startDate, double timeReceived) {
      Instant time = LocalDate.parse(data.get("dateTime").asText()).atStartOfDay(ZoneOffset.UTC).toInstant();
      JsonNode value = data.get("value");
      if (value == null || !value.isObject()) {
        return null;
      }
      String logType = data.get("logType").asText();
      FitbitSkinTemperature fitbitSkinTemperature = new FitbitSkinTemperature(
          time.toEpochMilli() / 1000d,
          timeReceived,
          (float) value.get("nightlyRelative").asDouble(),
          LOG_TYPE_MAP.getOrDefault(logType, FitbitSkinTemperatureLogType.UNKNOWN)
      );
      return new TopicData(time, skinTemperatureTopic, fitbitSkinTemperature);
    }
}
