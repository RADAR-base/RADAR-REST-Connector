package org.radarbase.connect.rest.fitbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.radarbase.connect.rest.fitbit.converter.AvroDataSingleton;
import org.radarcns.kafka.ObservationKey;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FitbitUser {
  private String fitbitUserId;
  private String key;
  private String refreshToken;
  private String projectId;
  private String userId;
  private String sourceId;
  private Instant startDate = Instant.parse("2017-01-01T00:00:00Z");
  private Instant endDate = Instant.MAX;
  private String accessToken;

  @JsonIgnore
  private SchemaAndValue observationKey;

  public FitbitUser() {
    // JSON create
  }

  public FitbitUser(String key, String fitbitUserId, String refreshToken, String projectId, String userId, String sourceId, Instant startDate, Instant endDate) {
    this.sourceId = sourceId;
    this.key = key;
    this.fitbitUserId = fitbitUserId;
    this.refreshToken = refreshToken;
    this.projectId = projectId;
    this.userId = userId;
    this.startDate = startDate;
    this.endDate = endDate;
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

  public String getKey() {
    return key;
  }

  public synchronized SchemaAndValue getObservationKey() {
    if (observationKey == null) {
      observationKey = AvroDataSingleton.getInstance().toConnectData(
          new ObservationKey(projectId, userId, sourceId));
    }
    return observationKey;
  }
}
