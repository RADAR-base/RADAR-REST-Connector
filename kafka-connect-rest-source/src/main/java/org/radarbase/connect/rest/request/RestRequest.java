/*
 * Copyright 2018 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.radarbase.connect.rest.request;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.kafka.connect.source.SourceRecord;

/**
 * Single request. This must originate from a RequestRoute and have a predefined source partition.
 */
public class RestRequest {
  private final Request request;
  private final Map<String, Object> partition;
  private final RequestRoute route;
  private final OkHttpClient client;
  private final Predicate<RestRequest> isValid;

  /**
   * Single RestRequest.
   * @param route originating route
   * @param client OkHttp client to make request with
   * @param request OkHttp request to make
   * @param partition Kafka source partition
   * @param isValid whether the request is still valid when the request is made. May be null.
   */
  public RestRequest(
      RequestRoute route,
      OkHttpClient client,
      Request request,
      Map<String, Object> partition,
      Predicate<RestRequest> isValid) {
    this.request = request;
    this.partition = partition;
    this.route = route;
    this.client = client;
    this.isValid = isValid;
  }

  public Request getRequest() {
    return request;
  }

  public Map<String, Object> getPartition() {
    return partition;
  }

  public boolean isStillValid() {
    return isValid == null || isValid.test(this);
  }

  /**
   * Handle the request using the internal client, using the request route converter.
   * @return stream of resulting source records, or {@code null} if the response was not successful.
   * @throws IOException if making or parsing the request failed.
   */
  public Stream<SourceRecord> handleRequest() throws IOException {
    if (!isStillValid()) {
      return Stream.empty();
    }

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
