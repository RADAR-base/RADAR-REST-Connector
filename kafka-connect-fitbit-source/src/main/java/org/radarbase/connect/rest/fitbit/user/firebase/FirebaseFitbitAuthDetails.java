package org.radarbase.connect.rest.fitbit.user.firebase;

import com.google.cloud.firestore.annotation.Exclude;
import com.google.cloud.firestore.annotation.IgnoreExtraProperties;
import com.google.cloud.firestore.annotation.PropertyName;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * POJO corresponding to the Fitbit Auth details document for a user in Firestore. Currently,
 * consists of OAuth 2 details, sourceId and date range for data collection.
 */
@IgnoreExtraProperties
public class FirebaseFitbitAuthDetails {

  protected static final String DEFAULT_SOURCE_ID = "fitbit";
  private String sourceId = getDefaultSourceId();
  private Date startDate;
  private Date endDate;
  private String version;

  private FitbitOAuth2UserCredentials oauth2Credentials;

  public FirebaseFitbitAuthDetails() {
    this.oauth2Credentials = new FitbitOAuth2UserCredentials();
    this.startDate = Date.from(Instant.parse("2017-01-01T00:00:00Z"));
    this.endDate = Date.from(Instant.parse("9999-12-31T23:59:59.999Z"));
    this.version = null;
  }

  @Exclude
  protected static String getDefaultSourceId() {
    return DEFAULT_SOURCE_ID + "-" + UUID.randomUUID();
  }

  @PropertyName("version")
  public String getVersion() {
    return version;
  }

  @PropertyName("version")
  public void setVersion(String version) {
    if (version != null && !version.trim().isEmpty()) {
      this.version = version;
    }
  }

  @PropertyName("start_date")
  public Date getStartDate() {
    return startDate;
  }

  @PropertyName("start_date")
  public void setStartDate(Date startDate) {
    if (startDate != null) {
      this.startDate = startDate;
    }
  }

  @PropertyName("end_date")
  public Date getEndDate() {
    return endDate;
  }

  @PropertyName("end_date")
  public void setEndDate(Date endDate) {
    if (endDate != null) {
      this.endDate = endDate;
    }
  }

  @PropertyName("source_id")
  public String getSourceId() {
    return sourceId;
  }

  @PropertyName("source_id")
  public void setSourceId(String sourceId) {
    if (sourceId != null && !sourceId.trim().isEmpty()) {
      this.sourceId = sourceId;
    }
  }

  @PropertyName("oauth2")
  public FitbitOAuth2UserCredentials getOauth2Credentials() {
    return oauth2Credentials;
  }

  @PropertyName("oauth2")
  public void setOauth2Credentials(FitbitOAuth2UserCredentials oauth2Credentials) {
    this.oauth2Credentials = oauth2Credentials;
  }
}
