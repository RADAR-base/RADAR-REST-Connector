package org.radarbase.connect.rest.converter;

import okhttp3.Response;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.config.RestSourceTool;
import org.radarbase.connect.rest.request.RestRequest;

import java.io.IOException;
import java.util.Collection;

public interface PayloadToSourceRecordConverter extends RestSourceTool {
  String TIMESTAMP_OFFSET_KEY = "timestamp";

  Collection<SourceRecord> convert(RestRequest request, Response response) throws IOException;
}
