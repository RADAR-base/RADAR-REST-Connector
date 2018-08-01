package org.radarbase.connect.rest.request;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.TIMESTAMP_OFFSET_KEY;

/** Loads a single URL. */
public class SimpleRequestGenerator implements RequestGenerator {
  private long lastTimestamp;
  private HttpUrl url;
  private String method;
  private RequestBody body;
  private long pollInterval;
  private Map<String, Object> key;
  private Headers headers;

  @Override
  public void start(RestSourceConnectorConfig config) {
    this.pollInterval = config.getPollInterval();

    this.url = HttpUrl.parse(config.getUrl());
    this.key = Collections.singletonMap("URL", config.getUrl());
    this.method = config.getMethod();

    Headers.Builder headersBuilder = new Headers.Builder();
    for (Map.Entry<String, String> header : config.getRequestProperties().entrySet()) {
      headersBuilder.add(header.getKey(), header.getValue());
    }
    this.headers = headersBuilder.build();

    if (config.getData() != null && !config.getData().isEmpty()) {
      String contentType = headers.get("Content-Type");
      MediaType mediaType;
      if (contentType == null) {
        mediaType = MediaType.parse("text/plain; charset=utf-8");
      } else {
        mediaType = MediaType.parse(contentType);
      }
      body = RequestBody.create(mediaType, config.getData());
    }
  }

  @Override
  public Stream<RestRequest> requests() throws InterruptedException {
    if (System.currentTimeMillis() - lastTimestamp < pollInterval) {
      Thread.sleep(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastTimestamp));
    }

    return Stream.of(new RestRequest(new Request.Builder()
        .method(method, body)
        .url(url)
        .headers(headers)
        .build(), key));
  }

  @Override
  public void requestSucceeded(RestProcessedResponse processedResponse) {
    lastTimestamp = (Long)processedResponse.getRecord().sourceOffset().get(TIMESTAMP_OFFSET_KEY);
  }

  @Override
  public void setOffsetStorageReader(OffsetStorageReader offsetStorageReader) {
    Map<String, Object> offset = offsetStorageReader.offset(key);
    if (offset != null) {
      lastTimestamp = (Long)offset.getOrDefault(TIMESTAMP_OFFSET_KEY, 0L);
    } else {
      lastTimestamp = 0L;
    }
  }

}
