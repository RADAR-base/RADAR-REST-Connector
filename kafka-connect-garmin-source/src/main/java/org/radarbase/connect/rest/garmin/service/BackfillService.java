package org.radarbase.connect.rest.garmin.service;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.request.RequestGenerator;
import org.radarbase.connect.rest.request.RestRequest;

/**
 * The backfill service should be used to collect historic data. This will send requests to garmin's
 * service to create backfill POST requests to our server.
 */
public class BackfillService implements RequestGenerator {
  private OffsetStorageReader offsetStorageReader;

  public BackfillService() {}

  /**
   * We don't wait long as these request don't respond with any data.
   * Hence no time required for processing. The limits on Garmin API are based on per minute.
   * So if we wait 1 minute we will pass the limit.
   */
  @Override
  public Instant getTimeOfNextRequest() {
    return Instant.now().plus(Duration.ofMinutes(1));
  }

  @Override
  public Stream<? extends RestRequest> requests() {
    return null;
  }

  @Override
  public void setOffsetStorageReader(OffsetStorageReader offsetStorageReader) {
    this.offsetStorageReader = offsetStorageReader;
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    // get the user repository and users
    // create backfill request for each user from their start date or stored offsets if present to
    // the backfill end date specified in the config
  }
}
