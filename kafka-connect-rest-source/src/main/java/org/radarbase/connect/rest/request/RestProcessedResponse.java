package org.radarbase.connect.rest.request;

import org.apache.kafka.connect.source.SourceRecord;

public class RestProcessedResponse {
  private final SourceRecord record;
  private final RestRequest request;
  private final RestResponse response;

  public RestProcessedResponse(RestResponse restResponse, SourceRecord record) {
    this.record = record;
    this.request = restResponse.getRequest();
    this.response = restResponse;
  }

  public RestRequest getRequest() {
    return request;
  }

  public RestResponse getResponse() {
    return response;
  }

  public SourceRecord getRecord() {
    return record;
  }
}
