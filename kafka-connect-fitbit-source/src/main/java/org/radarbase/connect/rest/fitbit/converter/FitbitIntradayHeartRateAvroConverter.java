package org.radarbase.connect.rest.fitbit.converter;

import static java.time.ZoneOffset.UTC;
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
  protected Stream<TopicData> processRecords(FitbitRestRequest request, JsonNode root,
      double timeReceived) {
    JsonNode intraday = root.get("activities-heart-intraday");
    if (intraday == null) {
      return Stream.empty();
    }

    JsonNode dataset = intraday.get("dataset");
    if (dataset == null) {
      return Stream.empty();
    }

    int interval = getRecordInterval(intraday, 1);
    ZonedDateTime startDate = request.getStartOffset().atZone(UTC);

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
