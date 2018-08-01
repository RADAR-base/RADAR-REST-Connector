package org.radarbase.connect.rest;

import okhttp3.OkHttpClient;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;
import org.radarbase.connect.rest.request.RequestGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import static org.radarbase.connect.rest.ThrowingFunction.tryOrNull;

public class RestSourceTask extends SourceTask {
  private static Logger log = LoggerFactory.getLogger(RestSourceTask.class);

  private PayloadToSourceRecordConverter converter;

  private OkHttpClient client;
  private RequestGenerator requestGenerator;

  @Override
  public void start(Map<String, String> map) {
    RestSourceConnectorConfig connectorConfig = new RestSourceConnectorConfig(map);
    converter = connectorConfig.getPayloadToSourceRecordConverter();
    converter.start(connectorConfig);
    client = new OkHttpClient();
    requestGenerator = connectorConfig.getRequestGenerator();
    requestGenerator.setOffsetStorageReader(context.offsetStorageReader());
    requestGenerator.start(connectorConfig);
  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    LongAdder requestsGenerated = new LongAdder();

    List<SourceRecord> requests = requestGenerator.requests()
        .peek(r -> requestsGenerated.increment())
        .map(tryOrNull(r -> r.withResponse(client.newCall(r.getRequest()).execute()),
          (r, ex) -> log.warn("Failed to make request: {}", ex.toString())))
        .filter(r -> {
          if (r == null) {
            return false;
          } else if (!r.getResponse().isSuccessful()) {
            log.warn("Failed to read data from {}", r);
            r.close();
            return false;
          } else {
            return true;
          }
        })
        .flatMap(tryOrNull(converter::convert,
            (r, ex) -> {
              r.close();
              log.warn("Failed to read incoming bytes of {}: {}", r, ex.toString());
            }))
        .filter(Objects::nonNull)
        .map(r -> {
          SourceRecord sourceRecord = r.getRecord();
          r.close();
          requestGenerator.requestSucceeded(r);
          return sourceRecord;
        })
        .collect(Collectors.toList());

    log.info("Processed {} records from {} URLs", requests.size(), requestsGenerated.sum());

    return requests;
  }

  @Override
  public void stop() {
    log.debug("Stopping source task");
  }

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }
}
