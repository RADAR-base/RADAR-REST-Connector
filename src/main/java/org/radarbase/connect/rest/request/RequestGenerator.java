package org.radarbase.connect.rest.request;

import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.config.RestSourceTool;

import java.util.stream.Stream;

/**
 * Dynamically generates requests. The requests should be based on the offsets that are stored in
 * the
 */
public interface RequestGenerator extends RestSourceTool {
  Stream<RestRequest> requests() throws InterruptedException;
  void requestSucceeded(RestProcessedResponse processedResponse);
  void setOffsetStorageReader(OffsetStorageReader offsetStorageReader);
}
