package org.radarbase.connect.rest.single;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;
import org.radarbase.connect.rest.request.RestProcessedResponse;
import org.radarbase.connect.rest.request.RestRequest;
import org.radarbase.connect.rest.request.RequestRoute;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.TIMESTAMP_OFFSET_KEY;

/** Loads a single URL. */
public class SingleRequestGenerator implements RequestRoute {
  private long lastTimestamp;
  private HttpUrl url;
  private String method;
  private RequestBody body;
  private long pollInterval;
  private Map<String, Object> key;
  private Headers headers;
  private PayloadToSourceRecordConverter converter;
  private OkHttpClient client;

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    SingleRestSourceConnectorConfig singleConfig = (SingleRestSourceConnectorConfig) config;
    this.pollInterval = config.getPollInterval();

    this.url = HttpUrl.parse(config.getUrl());
    this.key = Collections.singletonMap("URL", config.getUrl());
    this.method = singleConfig.getMethod();

    Headers.Builder headersBuilder = new Headers.Builder();
    for (Map.Entry<String, String> header : singleConfig.getRequestProperties().entrySet()) {
      headersBuilder.add(header.getKey(), header.getValue());
    }
    this.headers = headersBuilder.build();

    if (singleConfig.getData() != null && !singleConfig.getData().isEmpty()) {
      String contentType = headers.get("Content-Type");
      MediaType mediaType;
      if (contentType == null) {
        mediaType = MediaType.parse("text/plain; charset=utf-8");
      } else {
        mediaType = MediaType.parse(contentType);
      }
      body = RequestBody.create(mediaType, singleConfig.getData());
    }

    converter = config.getPayloadToSourceRecordConverter();
    client = new OkHttpClient();
  }

  @Override
  public long getTimeOfNextRequest() {
    return pollInterval - (System.currentTimeMillis() - lastTimestamp);
  }

  @Override
  public Stream<RestRequest> requests() {
    return Stream.of(new RestRequest(this, new Request.Builder()
        .method(method, body)
        .url(url)
        .headers(headers)
        .build(), key, client));
  }

  @Override
  public void requestSucceeded(RestProcessedResponse processedResponse) {
    lastTimestamp = (Long)processedResponse.getRecord().sourceOffset().get(TIMESTAMP_OFFSET_KEY);
  }

  @Override
  public void setOffsetStorageReader(OffsetStorageReader offsetStorageReader) {
    lastTimestamp = 0L;

    if (offsetStorageReader != null) {
      Map<String, Object> offset = offsetStorageReader.offset(key);
      if (offset != null) {
        lastTimestamp = (Long) offset.getOrDefault(TIMESTAMP_OFFSET_KEY, 0L);
      }
    }
  }

  @Override
  public PayloadToSourceRecordConverter converter() {
    return converter;
  }
}
