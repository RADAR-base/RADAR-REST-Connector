package org.radarbase.connect.rest.garmin.converter;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.stream.Stream;
import okhttp3.Headers;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;
import org.radarbase.connect.rest.request.RestRequest;

public abstract class GarminAvroConverter implements PayloadToSourceRecordConverter {
  @Override
  public Collection<SourceRecord> convert(RestRequest request, Headers headers, byte[] data) throws IOException {
    return null;
  }

  public abstract Stream<TopicData> processRecords(
      JsonNode root,
      double timeReceived);

  /** Single value for a topic. */
  protected static class TopicData {
    Instant sourceOffset;
    final String topic;
    final IndexedRecord value;

    public TopicData(Instant sourceOffset, String topic, IndexedRecord value) {
      this.sourceOffset = sourceOffset;
      this.topic = topic;
      this.value = value;
    }
  }
}


