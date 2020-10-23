package org.radarbase.connect.rest.garmin.user;

import io.confluent.connect.avro.AvroData;
import java.time.Instant;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.radarcns.kafka.ObservationKey;

public interface User {

  static SchemaAndValue computeObservationKey(AvroData avroData, User user) {
    return avroData.toConnectData(
        ObservationKey.getClassSchema(),
        new ObservationKey(user.getProjectId(), user.getUserId(), user.getSourceId()));
  }

  String getId();
  String getProjectId();
  String getUserId();
  String getSourceId();
  String getExternalUserId();
  Instant getStartDate();
  Instant getEndDate();
  String getVersion();
  boolean isAuthorized();


  default String getVersionedId() {
    String version = getVersion();
    if (version == null) {
      return getId();
    } else {
      return getId() + "#" + version;
    }
  }

  SchemaAndValue getObservationKey(AvroData avroData);

  default Boolean isComplete() {
    return getEndDate() != null
        && getStartDate() != null
        && getProjectId() != null
        && getUserId() != null
        && isAuthorized();
  }
}
