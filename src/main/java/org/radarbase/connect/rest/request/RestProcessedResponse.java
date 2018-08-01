package org.radarbase.connect.rest.request;

import org.apache.kafka.connect.source.SourceRecord;

public class RestProcessedResponse extends RestResponse {
  private final SourceRecord record;

  public RestProcessedResponse(RestResponse restResponse, SourceRecord record) {
    super(restResponse);
    this.record = record;
  }

  public SourceRecord getRecord() {
    return record;
  }
}
