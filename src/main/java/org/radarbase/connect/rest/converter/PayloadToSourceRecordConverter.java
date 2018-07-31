package org.radarbase.connect.rest.converter;

import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.apache.kafka.connect.source.SourceRecord;

import java.util.List;

public interface PayloadToSourceRecordConverter {
  List<SourceRecord> convert(final byte[] bytes) throws Exception;

  void start(RestSourceConnectorConfig config);
}
