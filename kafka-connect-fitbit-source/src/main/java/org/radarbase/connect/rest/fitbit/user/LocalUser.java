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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.confluent.connect.avro.AvroData;
import java.time.Instant;
import java.util.regex.Pattern;
import org.apache.kafka.connect.data.SchemaAndValue;

@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalUser implements User {
  private static final Pattern ILLEGAL_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");
  private String id;
  private String externalUserId;
  private String projectId;
  private String userId;
  private String sourceId;
  private Instant startDate = Instant.parse("2017-01-01T00:00:00Z");
  private Instant endDate = Instant.parse("9999-12-31T23:59:59.999Z");

  @JsonProperty("oauth2")
  private OAuth2UserCredentials oauth2Credentials = new OAuth2UserCredentials();

  @JsonIgnore
  private SchemaAndValue observationKey;

  @Override
  public String getId() {
    return id;
  }

  @JsonSetter
  public void setId(String id) {
    this.id = ILLEGAL_CHARACTERS_PATTERN.matcher(id).replaceAll("-");
  }

  public String getExternalUserId() {
    return externalUserId;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getUserId() {
    return userId;
  }

  public Instant getStartDate() {
    return startDate;
  }

  public Instant getEndDate() {
    return endDate;
  }

  public String getSourceId() {
    return sourceId;
  }

  public OAuth2UserCredentials getOAuth2Credentials() {
    return this.oauth2Credentials;
  }

  public void setOauth2Credentials(OAuth2UserCredentials oauth2Credentials) {
    this.oauth2Credentials = oauth2Credentials;
  }

  @JsonSetter("fitbitUserId")
  public void setFitbitUserId(String id) {
    this.externalUserId = id;
  }

  public LocalUser copy() {
    LocalUser copy = new LocalUser();
    copy.id = id;
    copy.externalUserId = externalUserId;
    copy.projectId = projectId;
    copy.userId = userId;
    copy.startDate = startDate;
    copy.endDate = endDate;
    copy.sourceId = sourceId;
    copy.oauth2Credentials = oauth2Credentials;
    return copy;
  }

  public synchronized SchemaAndValue getObservationKey(AvroData avroData) {
    if (observationKey == null) {
      observationKey = User.computeObservationKey(avroData, this);
    }
    return observationKey;
  }

  @Override
  public String toString() {
    return "LocalUser{" + "id='" + id + '\''
        + ", externalUserId='" + externalUserId + '\''
        + ", projectId='" + projectId + '\''
        + ", userId='" + userId + '\''
        + ", sourceId='" + sourceId + '\''
        + '}';
  }
}
