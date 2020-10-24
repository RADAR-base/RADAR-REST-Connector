package org.radarbase.connect.rest.garmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.avro.specific.SpecificRecord;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Epoch implements GarminData {
  private String userId;
  private String userAccessToken;
  private String summaryId;
  private String activityType;
  private Integer activeKilocalories;
  private Integer steps;
  private Float distanceInMeters;
  private Integer durationInSeconds;
  private Integer activeTimeInSeconds;
  private Integer startTimeInSeconds;
  private Integer startTimeOffsetInSeconds;
  private Float met;
  private String intensity;
  private Double meanMotionIntensity;
  private Double maxMotionIntensity;

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

  public String getActivityType() {
    return activityType;
  }

  public void setActivityType(String activityType) {
    this.activityType = activityType;
  }

  public Integer getActiveKilocalories() {
    return activeKilocalories;
  }

  public void setActiveKilocalories(Integer activeKilocalories) {
    this.activeKilocalories = activeKilocalories;
  }

  public Integer getSteps() {
    return steps;
  }

  public void setSteps(Integer steps) {
    this.steps = steps;
  }

  public Float getDistanceInMeters() {
    return distanceInMeters;
  }

  public void setDistanceInMeters(Float distanceInMeters) {
    this.distanceInMeters = distanceInMeters;
  }

  public Integer getDurationInSeconds() {
    return durationInSeconds;
  }

  public void setDurationInSeconds(Integer durationInSeconds) {
    this.durationInSeconds = durationInSeconds;
  }

  public Integer getActiveTimeInSeconds() {
    return activeTimeInSeconds;
  }

  public void setActiveTimeInSeconds(Integer activeTimeInSeconds) {
    this.activeTimeInSeconds = activeTimeInSeconds;
  }

  public Integer getStartTimeInSeconds() {
    return startTimeInSeconds;
  }

  public void setStartTimeInSeconds(Integer startTimeInSeconds) {
    this.startTimeInSeconds = startTimeInSeconds;
  }

  public Integer getStartTimeOffsetInSeconds() {
    return startTimeOffsetInSeconds;
  }

  public void setStartTimeOffsetInSeconds(Integer startTimeOffsetInSeconds) {
    this.startTimeOffsetInSeconds = startTimeOffsetInSeconds;
  }

  public Float getMet() {
    return met;
  }

  public void setMet(Float met) {
    this.met = met;
  }

  public String getIntensity() {
    return intensity;
  }

  public void setIntensity(String intensity) {
    this.intensity = intensity;
  }

  public Double getMeanMotionIntensity() {
    return meanMotionIntensity;
  }

  public void setMeanMotionIntensity(Double meanMotionIntensity) {
    this.meanMotionIntensity = meanMotionIntensity;
  }

  public Double getMaxMotionIntensity() {
    return maxMotionIntensity;
  }

  public void setMaxMotionIntensity(Double maxMotionIntensity) {
    this.maxMotionIntensity = maxMotionIntensity;
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
