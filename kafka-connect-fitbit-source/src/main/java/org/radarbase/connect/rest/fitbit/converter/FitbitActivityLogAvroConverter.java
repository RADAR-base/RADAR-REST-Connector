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
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarcns.connector.fitbit.FitbitActivityHeartRate;
import org.radarcns.connector.fitbit.FitbitActivityHeartRate.Builder;
import org.radarcns.connector.fitbit.FitbitActivityLevels;
import org.radarcns.connector.fitbit.FitbitActivityLogRecord;
import org.radarcns.connector.fitbit.FitbitManualDataEntry;
import org.radarcns.connector.fitbit.FitbitSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitActivityLogAvroConverter extends FitbitAvroConverter {
  private static final Logger logger = LoggerFactory.getLogger(FitbitActivityLogAvroConverter.class);
  private static final float FOOD_CAL_TO_KJOULE_FACTOR = 4.1868f;

  private String activityLogTopic;

  public FitbitActivityLogAvroConverter(AvroData avroData) {
    super(avroData);
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    activityLogTopic = ((FitbitRestSourceConnectorConfig)config).getActivityLogTopic();

    logger.info("Using activity log topic {}", activityLogTopic);
  }

  @Override
  protected Stream<TopicData> processRecords(
      FitbitRestRequest request, JsonNode root, double timeReceived) {

    JsonNode array = root.get("activities");
    if (array == null || !array.isArray()) {
      return Stream.empty();
    }

    return iterableToStream(array)
        .sorted(Comparator.comparing(s -> s.get("startTime").textValue()))
        .map(tryOrNull(s -> {
          OffsetDateTime startTime = OffsetDateTime.parse(s.get("startTime").textValue());
          FitbitActivityLogRecord record = getRecord(s, startTime);
          return new TopicData(startTime.toInstant(), activityLogTopic, record);
        }, (s, ex) -> logger.warn(
            "Failed to convert sleep patterns from request {} of user {}, {}",
            request.getRequest().url(), request.getUser(), s, ex)));
  }

  private FitbitActivityLogRecord getRecord(JsonNode s, OffsetDateTime startTime) {
    return FitbitActivityLogRecord.newBuilder()
        .setTime(startTime.toInstant().toEpochMilli() / 1000d)
        .setTimeReceived(System.currentTimeMillis() / 1000d)
        .setTimeLastModified(Instant.parse(s.get("lastModified").asText()).toEpochMilli() / 1000d)
        .setId(optLong(s, "logId").orElseThrow(
            () -> new IllegalArgumentException("Activity log ID not specified")))
        .setLogType(optString(s, "logType").orElse(null))
        .setType(optLong(s, "activityType").orElse(null))
        .setSpeed(optDouble(s, "speed").orElse(null))
        .setDistance(optDouble(s, "distance").map(Double::floatValue).orElse(null))
        .setSteps(optInt(s, "steps").orElse(null))
        .setEnergy(optInt(s, "calories").map(e -> e * FOOD_CAL_TO_KJOULE_FACTOR)
            .orElse(null))
        .setDuration(optLong(s, "duration").map(d -> d / 1000f).orElseThrow(
            () -> new IllegalArgumentException("Activity log duration not specified")))
        .setDurationActive(optLong(s, "duration").map(d -> d / 1000f).orElseThrow(
            () -> new IllegalArgumentException("Activity duration active not specified")))
        .setTimeZoneOffset(startTime.getOffset().getTotalSeconds())
        .setName(optString(s, "activityName").orElse(null))
        .setHeartRate(getHeartRate(s))
        .setManualDataEntry(getManualDataEntry(s))
        .setLevels(getActivityLevels(s))
        .setSource(getSource(s))
        .build();
  }

  private FitbitSource getSource(JsonNode s) {
    return optObject(s, "source")
        .flatMap(source -> optString(source, "id")
            .map(id -> FitbitSource.newBuilder()
                    .setId(id)
                    .setName(optString(source, "name").orElse(null))
                    .setType(optString(source, "type").orElse(null))
                    .setUrl(optString(source, "url").orElse(null))
                    .build()))
        .orElse(null);
  }

  private FitbitActivityLevels getActivityLevels(JsonNode s) {
    return optArray(s, "activityLevels")
        .map(levels -> {
          FitbitActivityLevels.Builder activityLevels = FitbitActivityLevels.newBuilder();
          for (JsonNode level : levels) {
            Integer time = optInt(level, "minutes")
                .map(t -> t * 60)
                .orElse(null);
            switch (optString(level, "name").orElse("")) {
              case "sedentary":
                activityLevels.setDurationSedentary(time);
                break;
              case "lightly":
                activityLevels.setDurationLightly(time);
                break;
              case "fairly":
                activityLevels.setDurationFairly(time);
                break;
              case "very":
                activityLevels.setDurationVery(time);
            }
          }

          return activityLevels.build();
        })
        .orElse(null);
  }

  private FitbitManualDataEntry getManualDataEntry(JsonNode s) {
    return optObject(s, "manualValuesSpecified")
        .map(manual -> FitbitManualDataEntry.newBuilder()
            .setSteps(optBoolean(manual, "steps").orElse(null))
            .setDistance(optBoolean(manual, "distance").orElse(null))
            .setEnergy(optBoolean(manual, "calorie").orElse(null))
            .build())
        .orElse(null);
  }

  private FitbitActivityHeartRate getHeartRate(JsonNode activity) {
    Optional<Integer> mean = optInt(activity, "averageHeartRate");
    Optional<Iterable<JsonNode>> zones = optArray(activity, "heartRateZones");

    if (mean.isEmpty() && zones.isEmpty()) {
      return null;
    }

    Builder heartRate = FitbitActivityHeartRate.newBuilder()
        .setMean(mean.orElse(null));

    zones.ifPresent(z -> {
      for (JsonNode zone : z) {
        switch (optString(zone, "name").orElse("")) {
          case "Out of Range":
            heartRate.setMin(optInt(zone, "min").orElse(null));
            heartRate.setDurationOutOfRange(optInt(zone, "minutes")
                .map(m -> m * 60)
                .orElse(null));
            break;
          case "Fat Burn":
            heartRate.setMinFatBurn(optInt(zone, "min").orElse(null));
            heartRate.setDurationFatBurn(optInt(zone, "minutes")
                .map(m -> m * 60)
                .orElse(null));
            break;
          case "Cardio":
            heartRate.setMinCardio(optInt(zone, "min").orElse(null));
            heartRate.setDurationCardio(optInt(zone, "minutes")
                .map(m -> m * 60)
                .orElse(null));
            break;
          case "Peak":
            heartRate.setMinPeak(optInt(zone, "min").orElse(null));
            heartRate.setMax(optInt(zone, "max").orElse(null));
            heartRate.setDurationPeak(optInt(zone, "minutes")
                .map(m -> m * 60)
                .orElse(null));
            break;
          default:
            logger.warn("Cannot process unknown heart rate zone {}", zone.get("name").asText());
            break;
        }
      }
    });

    return heartRate.build();
  }

}
