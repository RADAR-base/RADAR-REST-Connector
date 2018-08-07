package org.radarbase.connect.rest.request;

import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;

public interface RequestRoute extends RequestGenerator {
  PayloadToSourceRecordConverter converter();
  void requestSucceeded(RestRequest request, SourceRecord record);
  void requestEmpty(RestRequest request);
  void requestFailed(RestRequest request);
}
