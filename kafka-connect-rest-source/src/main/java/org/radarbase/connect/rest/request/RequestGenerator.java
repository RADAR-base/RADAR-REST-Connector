package org.radarbase.connect.rest.request;

import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.config.RestSourceTool;

import java.io.Closeable;
import java.util.stream.Stream;

/**
 * Dynamically generates requests. The requests should be based on the offsets that are stored in
 * the response SourceRecord.
 */
public interface RequestGenerator extends RestSourceTool {
  long getTimeOfNextRequest();
  Stream<? extends RestRequest> requests();
  void setOffsetStorageReader(OffsetStorageReader offsetStorageReader);
}
