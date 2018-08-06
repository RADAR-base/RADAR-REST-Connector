package org.radarbase.connect.rest.fitbit;

import org.apache.kafka.connect.data.SchemaAndValue;
import org.radarbase.connect.rest.fitbit.converter.AvroDataSingleton;
import org.radarcns.kafka.ObservationKey;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static org.radarbase.connect.rest.single.SingleRestSourceConnectorConfig.COLON_PATTERN;

public class FitbitUser {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      .withZone(ZoneId.systemDefault());

  private final String fitbitUserId;
  private final String key;
  private String refreshToken;
  private final String projectId;
  private final String userId;
  private final String sourceId;
  private final Instant startDate;
  private final Instant endDate;
  private String accessToken;
  private SchemaAndValue observationKey;

  public FitbitUser(String fitbitUserId, String refreshToken, String projectId, String userId, String sourceId, Instant startDate, Instant endDate) {
    this.sourceId = sourceId;
    this.key = fitbitUserId + ":" + userId;
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

  public static FitbitUser parse(String str) {
    String[] split = COLON_PATTERN.split(str);

    Instant startDate, endDate;

    try {
      startDate = Instant.from(DATE_FORMATTER.parse(split[5]));
    } catch (DateTimeParseException ex) {
      startDate = Instant.EPOCH;
    }

    try {
      endDate = Instant.from(DATE_FORMATTER.parse(split[6]));
    } catch (DateTimeParseException ex) {
      endDate = Instant.MAX;
    }

    return new FitbitUser(split[0], split[1], split[2], split[3], split[4], startDate, endDate);
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
