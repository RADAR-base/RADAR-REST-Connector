package org.radarbase.connect.rest.request;

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
  public long getTimeOfNextRequest() {
    return routes()
        .mapToLong(RequestRoute::getTimeOfNextRequest)
        .min()
        .orElse(Long.MAX_VALUE);
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
