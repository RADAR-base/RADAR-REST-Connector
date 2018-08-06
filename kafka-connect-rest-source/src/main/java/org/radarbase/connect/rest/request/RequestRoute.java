package org.radarbase.connect.rest.request;

import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;

public interface RequestRoute extends RequestGenerator {
  PayloadToSourceRecordConverter converter();
  void requestSucceeded(RestProcessedResponse processedResponse);
}
