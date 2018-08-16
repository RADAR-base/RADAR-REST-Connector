package org.radarbase.connect.rest.fitbit.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.confluent.connect.avro.AvroData;
import java.time.Instant;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalFitbitUser implements FitbitUser {
  private String id;
  private String fitbitUserId;
  private String projectId;
  private String userId;
  private String sourceId;
  private Instant startDate = Instant.parse("2017-01-01T00:00:00Z");
  private Instant endDate = Instant.MAX;
  private String accessToken;
  private String refreshToken;

  @JsonIgnore
  private SchemaAndValue observationKey;

  public LocalFitbitUser() {
    // JSON create
  }

  public LocalFitbitUser(String id, String fitbitUserId, String refreshToken, String projectId, String userId, String sourceId, Instant startDate, Instant endDate) {
    this.sourceId = sourceId;
    this.id = id;
    this.fitbitUserId = fitbitUserId;
    this.refreshToken = refreshToken;
    this.projectId = projectId;
    this.userId = userId;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  @Override
  public String getId() {
    return id;
  }

  public String getFitbitUserId() {
    return fitbitUserId;
  }

  public String getRefreshToken() {
    return refreshToken;
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

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
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
