package org.radarbase.connect.rest.request;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.kafka.connect.source.SourceRecord;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public class RestRequest {
  private final Request request;
  private final Map<String, Object> partition;
  private final RequestRoute route;
  private final OkHttpClient client;

  public RestRequest(RequestRoute route, Request request, Map<String, Object> partition,
      OkHttpClient client) {
    this.request = request;
    this.partition = partition;
    this.route = route;
    this.client = client;
  }

  public Request getRequest() {
    return request;
  }

  public Map<String, Object> getPartition() {
    return partition;
  }

  public Stream<SourceRecord> handleRequest() throws IOException {
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        route.requestFailed(this, response);
        return null;
      }

      Collection<SourceRecord> records = route.converter().convert(this, response);
      if (records.isEmpty()) {
        route.requestEmpty(this);
      } else {
        records.forEach(r -> route.requestSucceeded(this, r));
      }
      return records.stream();
    } catch (IOException ex) {
      route.requestFailed(this, null);
      throw ex;
    }
  }
}
