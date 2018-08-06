package org.radarbase.connect.rest.converter;

import org.radarbase.connect.rest.config.RestSourceTool;
import org.radarbase.connect.rest.request.RestProcessedResponse;
import org.radarbase.connect.rest.request.RestResponse;

import java.io.IOException;
import java.util.stream.Stream;

public interface PayloadToSourceRecordConverter extends RestSourceTool {
  String TIMESTAMP_OFFSET_KEY = "timestamp";

  Stream<RestProcessedResponse> convert(RestResponse restRequest) throws IOException;
}
