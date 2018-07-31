package org.radarbase.connect.rest.converter;

import org.radarbase.connect.rest.RestSinkConnectorConfig;
import org.apache.kafka.connect.sink.SinkRecord;

public interface SinkRecordToPayloadConverter {
  String convert(final SinkRecord record) throws Exception;

  void start(RestSinkConnectorConfig config);
}
