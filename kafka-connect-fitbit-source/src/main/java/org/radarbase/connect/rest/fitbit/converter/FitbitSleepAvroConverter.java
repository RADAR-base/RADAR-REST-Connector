package org.radarbase.connect.rest.fitbit.converter;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.ResponseBody;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.request.RestProcessedResponse;
import org.radarbase.connect.rest.request.RestResponse;
import org.radarcns.connector.fitbit.FitbitSleepStage;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.radarbase.connect.rest.fitbit.FitbitRequestGenerator.JSON_READER;
import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrRethrow;

public class FitbitSleepAvroConverter implements PayloadToSourceRecordConverter {
  private String sleepStageTopic;
  private final AvroDataSingleton avroData;

  public FitbitSleepAvroConverter() {
    avroData = AvroDataSingleton.getInstance();
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    sleepStageTopic = ((FitbitRestSourceConnectorConfig)config).getFitbitSleepStageTopic();
  }

  @Override
  public Stream<RestProcessedResponse> convert(RestResponse restRequest) throws IOException {
    ResponseBody body = restRequest.getResponse().body();
    if (body == null) {
      throw new IOException("Failed to read body");
    }
    JsonNode sleep = JSON_READER.readValue(body.charStream());

    JsonNode sleepArray = sleep.get("sleep");

    FitbitUser user = ((FitbitRestRequest) restRequest.getRequest()).getUser();
    SchemaAndValue key = user.getObservationKey();

    return StreamSupport.stream(sleepArray.spliterator(), false)
        .map(tryOrRethrow(s -> {
          // TODO: put correct data in here
          FitbitSleepStage.Builder sleepStageBuilder = FitbitSleepStage.newBuilder();

          SchemaAndValue sleepStage = avroData.toConnectData(sleepStageBuilder.build());

          // TODO: compute correct offset
          Map<String, Long> offset = Collections.singletonMap(
              TIMESTAMP_OFFSET_KEY, s.get("time???").asLong());

          return restRequest.withRecord(new SourceRecord(
              restRequest.getRequest().getPartition(), offset, sleepStageTopic,
              key.schema(), key.value(), sleepStage.schema(), sleepStage.value()));
        }, (a, ex) -> new RuntimeException("Failed to parse response", ex)));
  }
}
