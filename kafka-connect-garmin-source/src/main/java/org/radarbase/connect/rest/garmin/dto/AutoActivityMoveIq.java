package org.radarbase.connect.rest.garmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.avro.specific.SpecificRecord;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutoActivityMoveIq implements GarminData {
  private String userId;
  private String userAccessToken;
  private String summaryId;
  private String deviceName;
  private String calendarDate;
  private Integer startTimeInSeconds;
  private Integer durationInSeconds;
  private String activityType;
  private String activitySubType;
  private Integer offsetInSeconds;

  public String getUserAccessToken() {
    return userAccessToken;
  }

  public void setUserAccessToken(String userAccessToken) {
    this.userAccessToken = userAccessToken;
  }

  public String getSummaryId() {
    return summaryId;
  }

  public void setSummaryId(String summaryId) {
    this.summaryId = summaryId;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName;
  }

  public String getCalendarDate() {
    return calendarDate;
  }

  public void setCalendarDate(String calendarDate) {
    this.calendarDate = calendarDate;
  }

  public Integer getStartTimeInSeconds() {
    return startTimeInSeconds;
  }

  public void setStartTimeInSeconds(Integer startTimeInSeconds) {
    this.startTimeInSeconds = startTimeInSeconds;
  }

  public Integer getDurationInSeconds() {
    return durationInSeconds;
  }

  public void setDurationInSeconds(Integer durationInSeconds) {
    this.durationInSeconds = durationInSeconds;
  }

  public String getActivityType() {
    return activityType;
  }

  public void setActivityType(String activityType) {
    this.activityType = activityType;
  }

  public String getActivitySubType() {
    return activitySubType;
  }

  public void setActivitySubType(String activitySubType) {
    this.activitySubType = activitySubType;
  }

  public Integer getOffsetInSeconds() {
    return offsetInSeconds;
  }

  public void setOffsetInSeconds(Integer offsetInSeconds) {
    this.offsetInSeconds = offsetInSeconds;
  }

  @Override
  public SpecificRecord toAvroRecord() {
    return null;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
}
