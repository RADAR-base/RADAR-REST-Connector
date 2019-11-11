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
import org.radarcns.connector.fitbit.FitbitIntradayCalories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitIntradayCaloriesAvroConverter extends FitbitAvroConverter {

  private static final Logger logger =
      LoggerFactory.getLogger(FitbitIntradayCaloriesAvroConverter.class);

  private String caloriesTopic;

  public FitbitIntradayCaloriesAvroConverter(AvroData avroData) {
    super(avroData);
  }

  @Override
  protected Stream<TopicData> processRecords(
      FitbitRestRequest request, JsonNode root, double timeReceived) {
    JsonNode intraday = root.get("activities-calories-intraday");
    if (intraday == null) {
      return Stream.empty();
    }

    JsonNode dataset = intraday.get("dataset");
    if (dataset == null) {
      return Stream.empty();
    }

    int interval = getRecordInterval(intraday, 60);

    // Used as the date to convert the local times in the dataset to absolute times.
    ZonedDateTime startDate = request.getDateRange().end();

    return iterableToStream(dataset)
        .map(
            tryOrNull(
                activity -> {
                  Instant time =
                      startDate.with(LocalTime.parse(activity.get("time").asText())).toInstant();

                  FitbitIntradayCalories calories =
                      new FitbitIntradayCalories(
                          time.toEpochMilli() / 1000d,
                          timeReceived,
                          interval,
                          activity.get("value").asDouble(),
                          activity.get("level").asInt(),
                          activity.get("mets").asDouble());

                  return new TopicData(time, caloriesTopic, calories);
                },
                (a, ex) ->
                    logger.warn(
                        "Failed to convert steps from request {} of user {}, {}",
                        request.getRequest().url(),
                        request.getUser(),
                        a,
                        ex)));
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    caloriesTopic = ((FitbitRestSourceConnectorConfig) config).getFitbitIntradayCaloriesTopic();
    logger.info("Using calories topic {}", caloriesTopic);
  }
}
