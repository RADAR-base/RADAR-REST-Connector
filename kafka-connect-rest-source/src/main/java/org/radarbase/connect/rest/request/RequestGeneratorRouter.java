package org.radarbase.connect.rest.request;

import org.apache.kafka.connect.storage.OffsetStorageReader;

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
        .max()
        .orElse(Long.MAX_VALUE);
  }

  public abstract Stream<RequestRoute> routes();

  public void setOffsetStorageReader(OffsetStorageReader offsetStorageReader) {
    routes().forEach(r -> r.setOffsetStorageReader(offsetStorageReader));
  }
}
