package org.radarbase.connect.rest.fitbit.converter;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.ResponseBody;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitUser;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.request.RestProcessedResponse;
import org.radarbase.connect.rest.request.RestResponse;
import org.radarcns.connector.fitbit.FitbitIntradaySteps;
import org.radarcns.kafka.ObservationKey;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.radarbase.connect.rest.fitbit.FitbitRequestGenerator.JSON_READER;
import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrRethrow;

public class FitbitIntradayStepsAvroConverter implements PayloadToSourceRecordConverter {
  private String stepTopic;
  private final AvroDataSingleton avroData;

  public FitbitIntradayStepsAvroConverter() {
    avroData = AvroDataSingleton.getInstance();
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    stepTopic = ((FitbitRestSourceConnectorConfig)config).getFitbitIntradayStepsTopic();
  }

  @Override
  public Stream<RestProcessedResponse> convert(RestResponse restRequest) throws IOException {
    ResponseBody body = restRequest.getResponse().body();
    if (body == null) {
      throw new IOException("Failed to read body");
    }
    JsonNode activities = JSON_READER.readValue(body.charStream());

    JsonNode activitiesArray = activities.get("activities");

    FitbitUser user = ((FitbitRestRequest) restRequest.getRequest()).getUser();
    SchemaAndValue key = avroData.toConnectData(
        new ObservationKey(user.getProjectId(), user.getUserId(), user.getSourceId()));

    return StreamSupport.stream(activitiesArray.spliterator(), false)
        .map(tryOrRethrow(activity -> {
          // TODO: put correct data in here
          FitbitIntradaySteps.Builder stepBuilder = FitbitIntradaySteps.newBuilder();

          SchemaAndValue steps = avroData.toConnectData(stepBuilder.build());

          // TODO: compute correct offset
          Map<String, Long> offset = Collections.singletonMap(
              TIMESTAMP_OFFSET_KEY, activity.get("time???").asLong());

          return restRequest.withRecord(new SourceRecord(
              restRequest.getRequest().getPartition(), offset, stepTopic,
              key.schema(), key.value(), steps.schema(), steps.value()));
        }, (a, ex) -> new RuntimeException("Failed to parse response", ex)));
  }
}
