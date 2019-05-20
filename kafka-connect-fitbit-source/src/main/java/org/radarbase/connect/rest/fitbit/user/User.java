/*
 * Copyright 2018 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.radarbase.connect.rest.fitbit.user;

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

  String getExternalUserId();

  String getProjectId();

  String getUserId();

  Instant getStartDate();

  Instant getEndDate();

  String getSourceId();

  SchemaAndValue getObservationKey(AvroData avroData);

  default Boolean isComplete() {
    return getEndDate() != null
        && getStartDate() != null
        && getProjectId() != null
        && getUserId() != null;
  }
}
