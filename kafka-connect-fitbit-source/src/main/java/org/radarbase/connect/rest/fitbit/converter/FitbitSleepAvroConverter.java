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

import static org.radarbase.connect.rest.fitbit.route.FitbitSleepRoute.DATE_TIME_FORMAT;
import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.connect.avro.AvroData;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.avro.generic.IndexedRecord;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarcns.connector.fitbit.FitbitSleepClassic;
import org.radarcns.connector.fitbit.FitbitSleepClassicLevel;
import org.radarcns.connector.fitbit.FitbitSleepStage;
import org.radarcns.connector.fitbit.FitbitSleepStageLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitSleepAvroConverter extends FitbitAvroConverter {
  private static final Logger logger = LoggerFactory.getLogger(FitbitSleepAvroConverter.class);

  private static final Map<String, FitbitSleepClassicLevel> CLASSIC_MAP = new HashMap<>();
  private static final Map<String, FitbitSleepStageLevel> STAGES_MAP = new HashMap<>();

  static {
    CLASSIC_MAP.put("awake", FitbitSleepClassicLevel.AWAKE);
    CLASSIC_MAP.put("asleep", FitbitSleepClassicLevel.ASLEEP);
    CLASSIC_MAP.put("restless", FitbitSleepClassicLevel.RESTLESS);

    STAGES_MAP.put("wake", FitbitSleepStageLevel.AWAKE);
    STAGES_MAP.put("rem", FitbitSleepStageLevel.REM);
    STAGES_MAP.put("deep", FitbitSleepStageLevel.DEEP);
    STAGES_MAP.put("light", FitbitSleepStageLevel.LIGHT);
  }

  private String sleepStagesTopic;
  private String sleepClassicTopic;

  public FitbitSleepAvroConverter(AvroData avroData) {
    super(avroData);
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    sleepStagesTopic = ((FitbitRestSourceConnectorConfig) config).getFitbitSleepStagesTopic();
    sleepClassicTopic = ((FitbitRestSourceConnectorConfig) config).getFitbitSleepClassicTopic();

    logger.info("Using sleep topic {} and {}", sleepStagesTopic, sleepClassicTopic);
  }

  @Override
  protected Stream<TopicData> processRecords(
      FitbitRestRequest request, JsonNode root, double timeReceived) {
    JsonNode meta = root.get("meta");
    if (meta != null) {
      JsonNode state = meta.get("state");
      if (state != null && meta.get("state").asText().equals("pending")) {
        return Stream.empty();
      }
    }
    JsonNode sleepArray = root.get("sleep");
    if (sleepArray == null) {
      return Stream.empty();
    }

    return iterableToStream(sleepArray)
        .sorted(Comparator.comparing(s -> s.get("startTime").asText()))
        .flatMap(tryOrNull(s -> {
          Instant startTime = Instant.from(DATE_TIME_FORMAT.parse(s.get("startTime").asText()));
          boolean isStages = s.get("type") == null || s.get("type").asText().equals("stages");
          int efficiency = s.has("efficiency") ? s.get("efficiency").asInt() : null;

          // use an intermediate offset for all records but the last. Since the query time
          // depends only on the start time of a sleep stages group, this will reprocess the entire
          // sleep stages group if something goes wrong while processing.
          Instant intermediateOffset = startTime.minus(Duration.ofSeconds(1));

          List<TopicData> allRecords = iterableToStream(s.get("levels").get("data"))
              .map(d -> {
                IndexedRecord sleep;
                String topic;

                String dateTime = d.get("dateTime").asText();
                int duration = d.get("seconds").asInt();
                String level = d.get("level").asText();

                if (isStages) {
                  sleep = new FitbitSleepStage(
                      dateTime,
                      timeReceived,
                      duration,
                      STAGES_MAP.getOrDefault(level, FitbitSleepStageLevel.UNKNOWN),
                      efficiency);
                  topic = sleepStagesTopic;
                } else {
                  sleep = new FitbitSleepClassic(
                      dateTime,
                      timeReceived,
                      duration,
                      CLASSIC_MAP.getOrDefault(level, FitbitSleepClassicLevel.UNKNOWN),
                      efficiency);
                  topic = sleepClassicTopic;
                }

                return new TopicData(intermediateOffset, topic, sleep);
              })
              .collect(Collectors.toList());

          if (allRecords.isEmpty()) {
            return Stream.empty();
          }

          // The final group gets the actual offset, to ensure that the group does not get queried
          // again.
          allRecords.get(allRecords.size() - 1).sourceOffset = startTime;

          return allRecords.stream();
        }, (s, ex) -> logger.warn(
            "Failed to convert sleep patterns from request {} of user {}, {}",
            request.getRequest().url(), request.getUser(), s, ex)));
  }
}
