package org.radarbase.connect.rest.converter;

import okhttp3.ResponseBody;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.request.RestProcessedResponse;
import org.radarbase.connect.rest.request.RestResponse;
import org.radarbase.connect.rest.selector.TopicSelector;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;

public class BytesPayloadConverter implements PayloadToSourceRecordConverter {
  private TopicSelector topicSelector;

  // Just bytes for incoming messages
  @Override
  public Stream<RestProcessedResponse> convert(RestResponse request) throws IOException {
    Map<String, Long> sourceOffset = Collections.singletonMap(
        TIMESTAMP_OFFSET_KEY, currentTimeMillis());
    ResponseBody body = request.getResponse().body();
    return Stream.of(request.withRecord(
        new SourceRecord(request.getPartition(), sourceOffset, topicSelector.getTopic(request),
            Schema.BYTES_SCHEMA, body != null ? body.bytes() : null)));
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    topicSelector = config.getTopicSelector();
  }
}
