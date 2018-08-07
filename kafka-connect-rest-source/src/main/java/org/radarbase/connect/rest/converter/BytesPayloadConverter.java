package org.radarbase.connect.rest.converter;

import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.request.RestRequest;
import org.radarbase.connect.rest.selector.TopicSelector;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

public class BytesPayloadConverter implements PayloadToSourceRecordConverter {
  private TopicSelector topicSelector;

  // Just bytes for incoming messages
  @Override
  public Collection<SourceRecord> convert(RestRequest request, Response response) throws IOException {
    Map<String, Long> sourceOffset = Collections.singletonMap(
        TIMESTAMP_OFFSET_KEY, currentTimeMillis());
    ResponseBody body = response.body();
    byte[] result = body != null ? body.bytes() : null;
    String topic = topicSelector.getTopic(request, result);
    return Collections.singleton(
        new SourceRecord(request.getPartition(), sourceOffset,
            topic, Schema.BYTES_SCHEMA, result));
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    topicSelector = config.getTopicSelector();
  }
}
