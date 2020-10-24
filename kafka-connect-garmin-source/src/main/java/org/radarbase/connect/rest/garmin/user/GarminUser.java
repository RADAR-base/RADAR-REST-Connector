package org.radarbase.connect.rest.garmin.user;

import io.confluent.connect.avro.AvroData;
import java.time.Instant;
import org.apache.kafka.connect.data.SchemaAndValue;

public class GarminUser implements User{

  @Override
  public String getId() {
    return null;
  }

  @Override
  public String getProjectId() {
    return null;
  }

  @Override
  public String getUserId() {
    return null;
  }

  @Override
  public String getSourceId() {
    return null;
  }

  @Override
  public String getExternalUserId() {
    return null;
  }

  @Override
  public Instant getStartDate() {
    return null;
  }

  @Override
  public Instant getEndDate() {
    return null;
  }

  @Override
  public String getVersion() {
    return null;
  }

  @Override
  public boolean isAuthorized() {
    return false;
  }

  @Override
  public SchemaAndValue getObservationKey(AvroData avroData) {
    return null;
  }
}
