package org.radarbase.connect.rest.fitbit.converter;

import static org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator.JSON_READER;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.connect.avro.AvroData;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.request.RestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitIntradayStepsAvroConverter implements PayloadToSourceRecordConverter {
  private static final Logger logger = LoggerFactory.getLogger(FitbitIntradayStepsAvroConverter.class);

  private String stepTopic;
  private final AvroData avroData;

  public FitbitIntradayStepsAvroConverter(AvroData avroData) {
    this.avroData = avroData;
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    stepTopic = ((FitbitRestSourceConnectorConfig)config).getFitbitIntradayStepsTopic();
  }

  @Override
  public Collection<SourceRecord> convert(RestRequest restRequest, Response response) throws IOException {
    ResponseBody body = response.body();
    if (body == null) {
      throw new IOException("Failed to read body");
    }
    JsonNode activities = JSON_READER.readTree(body.charStream());
    logger.info("Activities: {}", activities);
    return Collections.emptyList();

//    JsonNode activitiesArray = activities.get("activities");
//
//    FitbitUser user = ((FitbitRestRequest) restRequest.getRequest()).getUser();
//    SchemaAndValue key = avroData.toConnectData(
//        new ObservationKey(user.getProjectId(), user.getUserId(), user.getSourceId()));
//
//    return StreamSupport.stream(activitiesArray.spliterator(), false)
//        .map(tryOrRethrow(activity -> {
//          logger.info
//          // TODO: put correct data in here
//          FitbitIntradaySteps.Builder stepBuilder = FitbitIntradaySteps.newBuilder();
//
//          SchemaAndValue steps = avroData.toConnectData(stepBuilder.build());
//
//          // TODO: compute correct offset
//          Map<String, Long> offset = Collections.singletonMap(
//              TIMESTAMP_OFFSET_KEY, activity.get("time???").asLong());
//
//          return restRequest.withRecord(new SourceRecord(
//              restRequest.getRequest().getPartition(), offset, stepTopic,
//              key.schema(), key.value(), steps.schema(), steps.value()));
//        }, (a, ex) -> new RuntimeException("Failed to parse response", ex)));
  }
}
