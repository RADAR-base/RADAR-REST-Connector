package org.radarbase.connect.rest.request;

import okhttp3.Response;
import org.apache.kafka.connect.source.SourceRecord;

import java.io.Closeable;

public class RestResponse implements Closeable {
  private final Response response;
  private final RestRequest request;

  public RestResponse(RestRequest restRequest, Response response) {
    this.request = restRequest;
    this.response = response;
  }

  public RestResponse(RestResponse restResponse) {
    this(restResponse.request, restResponse.response);
  }

  public Response getResponse() {
    return response;
  }

  public RestRequest getRequest() {
    return request;
  }

  public RestProcessedResponse withRecord(SourceRecord record) {
    return new RestProcessedResponse(this, record);
  }

  @Override
  public String toString() {
    return "RestResponse{"
      + "request=" + request.getRequest()
      + "response=" + response
      + '}';
  }

  @Override
  public void close() {
    response.close();
  }
}
