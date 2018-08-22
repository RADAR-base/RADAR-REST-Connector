package org.radarbase.connect.rest.request;

import java.time.Instant;
import java.util.Comparator;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

import java.util.stream.Stream;

public abstract class RequestGeneratorRouter implements RequestGenerator {
  @Override
  public Stream<RestRequest> requests() {
    return routes()
        .flatMap(RequestRoute::requests);
  }

  @Override
  public Instant getTimeOfNextRequest() {
    return routes()
        .map(RequestRoute::getTimeOfNextRequest)
        .min(Comparator.naturalOrder())
        .orElse(Instant.MAX);
  }

  public abstract Stream<RequestRoute> routes();

  public void setOffsetStorageReader(OffsetStorageReader offsetStorageReader) {
    routes().forEach(r -> r.setOffsetStorageReader(offsetStorageReader));
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    routes().forEach(r -> r.initialize(config));
  }
}
