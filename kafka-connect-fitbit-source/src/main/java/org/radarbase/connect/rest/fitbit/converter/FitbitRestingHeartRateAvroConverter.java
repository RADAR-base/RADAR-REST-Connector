/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.connect.rest.fitbit.converter;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.connect.avro.AvroData;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarcns.connector.fitbit.FitbitRestingHeartRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FitbitRestingHeartRateAvroConverter extends FitbitAvroConverter {
  private static final Logger logger = LoggerFactory.getLogger(
      FitbitRestingHeartRateAvroConverter.class);

  private String restingHeartRateTopic;

  public FitbitRestingHeartRateAvroConverter(AvroData avroData) {
    super(avroData);
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    restingHeartRateTopic =
        ((FitbitRestSourceConnectorConfig) config).getFitbitRestingHeartRateTopic();
    logger.info("Using resting heart rate topic {}", restingHeartRateTopic);
  }

  @Override
  protected Stream<TopicData> processRecords(
      FitbitRestRequest request, JsonNode root, double timeReceived) {
    JsonNode activitiesHeart = root.get("activities-heart");
    if (activitiesHeart == null || !activitiesHeart.isArray() || activitiesHeart.size() == 0) {
      logger.info("No resting heart rate available from {} on the specified date", request.getRequest().url());
      return Stream.empty();
    }

    return StreamSupport.stream(activitiesHeart.spliterator(), false)
        .filter(entry -> entry != null && entry.isObject())
        .map(entry -> {
          JsonNode dateTimeNode = entry.get("dateTime");
          if (dateTimeNode == null) {
            logger.warn("Failed to get resting heart rate from {}, {} : the 'dateTime' node is missing.", request.getRequest().url(), root);
            return null;
          }
          String date = dateTimeNode.asText();

          JsonNode value = entry.get("value");
          if (value == null || !value.isObject()) {
            logger.warn("Failed to get resting heart rate from {}, {} : the 'value' node is missing.", request.getRequest().url(), root);
            return null;
          }

          JsonNode restingHeartRateNode = value.get("restingHeartRate");
          if (restingHeartRateNode == null) {
            logger.warn("Failed to get resting heart rate from {}, {} : the 'restingHeartRate' node is missing.", request.getRequest().url(), root);
            return null;
          }
          int restingHeartRate = restingHeartRateNode.asInt();

          FitbitRestingHeartRate fitbitRestingHeartRate
              = new FitbitRestingHeartRate(date, timeReceived, restingHeartRate);

          return new TopicData(request.getDateRange().start().toInstant(),
              restingHeartRateTopic, fitbitRestingHeartRate);
        })
        .filter(java.util.Objects::nonNull);
  }
}
