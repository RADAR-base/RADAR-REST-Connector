package org.radarbase.connect.rest.request;

import java.time.Instant;
import java.util.stream.Stream;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.config.RestSourceTool;

/**
 * Dynamically generates requests. The requests should be based on the offsets that are stored in
 * the response SourceRecord.
 */
public interface RequestGenerator extends RestSourceTool {
  Instant getTimeOfNextRequest();

  /**
   * Requests that should be queried next.
   */
  Stream<? extends RestRequest> requests();

  /**
   * Set the Kafka offset storage reader. This allows for resetting the request intervals.
   * @param offsetStorageReader possibly null offset storage reader.
   */
  void setOffsetStorageReader(OffsetStorageReader offsetStorageReader);
}
