package org.radarbase.connect.rest.request;

import okhttp3.Response;
import org.apache.kafka.connect.source.SourceRecord;

import java.io.Closeable;

public class RestResponse extends RestRequest implements Closeable {
  private final Response response;

  public RestResponse(RestRequest restRequest, Response response) {
    super(restRequest.getRequest());
    this.response = response;
  }

  public RestResponse(RestResponse restResponse) {
    super(restResponse);
    this.response = restResponse.response;
  }

  public Response getResponse() {
    return response;
  }

  public RestProcessedResponse withRecord(SourceRecord record) {
    return new RestProcessedResponse(this, record);
  }

  @Override
  public String toString() {
    return "RestResponse{"
      + "request=" + getRequest()
      + "response=" + response
      + '}';
  }

  @Override
  public void close() {
    response.close();
  }
}
