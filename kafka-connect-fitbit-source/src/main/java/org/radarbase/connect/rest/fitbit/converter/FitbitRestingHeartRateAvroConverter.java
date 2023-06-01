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
    JsonNode resting = root.get("activities-heart");
    if (resting == null || !resting.isObject()) {
      logger.info("No resting heart rate available from {} on the specified date", request.getRequest().url());
      return Stream.empty();
    }

    JsonNode dateTimeNode = resting.get("dateTime");
    if (dateTimeNode == null) {
      logger.warn("Failed to get resting heart rate from {}, {} : the 'dateTime' node is missing.", request.getRequest().url(), root);
      return Stream.empty();
    }
    String date = dateTimeNode.asText();

    JsonNode value = resting.get("value");
    if (value == null || !value.isObject()) {
      logger.warn("Failed to get resting heart rate from {}, {} : the 'value' node is missing.", request.getRequest().url(), root);
      return Stream.empty();
    }

    JsonNode restingHeartRateNode = value.get("restingHeartRate");
    if (restingHeartRateNode == null) {
      logger.warn("Failed to get resting heart rate from {}, {} : the 'restingHeartRate' node is missing.", request.getRequest().url(), root);
      return Stream.empty();
    }
    int restingHeartRate = restingHeartRateNode.asInt();

    FitbitRestingHeartRate fitbitRestingHeartRate
        = new FitbitRestingHeartRate(date, timeReceived, restingHeartRate);

    return Stream.of(new TopicData(request.getDateRange().start().toInstant(),
        restingHeartRateTopic, fitbitRestingHeartRate));
  }
}
