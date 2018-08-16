package org.radarbase.connect.rest.fitbit.user;

import io.confluent.connect.avro.AvroData;
import java.time.Instant;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.radarcns.kafka.ObservationKey;

public interface FitbitUser {
  String getId();
  String getFitbitUserId();
  String getProjectId();
  String getUserId();
  Instant getStartDate();
  Instant getEndDate();
  String getSourceId();
  String getAccessToken();
  SchemaAndValue getObservationKey(AvroData avroData);

  static SchemaAndValue computeObservationKey(AvroData avroData, FitbitUser user) {
    return avroData.toConnectData(
          ObservationKey.getClassSchema(),
          new ObservationKey(user.getProjectId(), user.getUserId(), user.getSourceId()));
  }
}
