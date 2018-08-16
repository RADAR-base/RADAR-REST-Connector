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

public class FitbitSleepAvroConverter implements PayloadToSourceRecordConverter {
  private static final Logger logger = LoggerFactory.getLogger(FitbitSleepAvroConverter.class);

  private String sleepStageTopic;
  private final AvroData avroData;

  public FitbitSleepAvroConverter(AvroData avroData) {
    this.avroData = avroData;
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    sleepStageTopic = ((FitbitRestSourceConnectorConfig)config).getFitbitSleepStageTopic();
  }

  @Override
  public Collection<SourceRecord> convert(RestRequest restRequest, Response response) throws IOException {
    ResponseBody body = response.body();
    if (body == null) {
      throw new IOException("Failed to read body");
    }
    JsonNode sleep = JSON_READER.readTree(body.charStream());
    logger.info("Sleep: {}", sleep);
    return Collections.emptyList();
//
//    JsonNode sleepArray = sleep.get("sleep");
//
//    FitbitUser user = ((FitbitRestRequest) restRequest.getRequest()).getUser();
//    SchemaAndValue key = user.getObservationKey();
//
//    return StreamSupport.stream(sleepArray.spliterator(), false)
//        .map(tryOrRethrow(s -> {
//          // TODO: put correct data in here
//          FitbitSleepStage.Builder sleepStageBuilder = FitbitSleepStage.newBuilder();
//
//          SchemaAndValue sleepStage = avroData.toConnectData(sleepStageBuilder.build());
//
//          // TODO: compute correct offset
//          Map<String, Long> offset = Collections.singletonMap(
//              TIMESTAMP_OFFSET_KEY, s.get("time???").asLong());
//
//          return restRequest.withRecord(new SourceRecord(
//              restRequest.getRequest().getPartition(), offset, sleepStageTopic,
//              key.schema(), key.value(), sleepStage.schema(), sleepStage.value()));
//        }, (a, ex) -> new RuntimeException("Failed to parse response", ex)));
  }
}
