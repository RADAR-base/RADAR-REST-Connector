package org.radarbase.connect.rest.fitbit.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.confluent.connect.avro.AvroData;
import java.time.Instant;
import java.util.regex.Pattern;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.OAuth2UserCredentials;

@JsonInclude(Include.NON_EMPTY)
//@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalFitbitUser implements FitbitUser {
  private static final Pattern ILLEGAL_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");
  private String id;
  private String fitbitUserId;
  private String projectId;
  private String userId;
  private String sourceId;
  private Instant startDate = Instant.parse("2017-01-01T00:00:00Z");
  private Instant endDate = Instant.MAX;

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

  public String getFitbitUserId() {
    return fitbitUserId;
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

  public synchronized SchemaAndValue getObservationKey(AvroData avroData) {
    if (observationKey == null) {
      observationKey = FitbitUser.computeObservationKey(avroData, this);
    }
    return observationKey;
  }

  @Override
  public String toString() {
    return "LocalFitbitUser{" + "id='" + id + '\''
        + ", fitbitUserId='" + fitbitUserId + '\''
        + ", projectId='" + projectId + '\''
        + ", userId='" + userId + '\''
        + ", sourceId='" + sourceId + '\''
        + '}';
  }
}
