package org.radarbase.connect.rest.garmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.avro.specific.SpecificRecord;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Activity implements GarminData {

  private String userId;
  private String userAccessToken;
  private String summaryId;
  private Boolean isParent;
  private String parentSummaryId;
  private Integer durationInSeconds;
  private Integer startTimeInSeconds;
  private Integer startTimeOffsetInSeconds;
  private String activityType;
  private Float averageBikeCadenceInRoundsPerMinute;
  private Integer averageHeartRateInBeatsPerMinute;
  private Float averageRunCadenceInStepsPerMinute;
  private Float averageSpeedInMetersPerSecond;
  private Float averageSwimCadenceInStrokesPerMinute;
  private Float averagePaceInMinutesPerKilometer;
  private Integer activeKilocalories;
  private String deviceName;
  private Float distanceInMeters;
  private Float maxBikeCadenceInRoundsPerMinute;
  private Integer maxHeartRateInBeatsPerMinute;
  private Float maxPaceInMinutesPerKilometer;
  private Float maxRunCadenceInStepsPerMinute;
  private Float maxSpeedInMetersPerSecond;
  private Integer numberOfActiveLengths;
  private Double startingLatitudeInDegree;
  private Double startingLongitudeInDegree;
  private Integer steps;
  private Float totalElevationGainInMeters;
  private Float totalElevationLossInMeters;
  private Boolean manual;

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

  public Boolean getIsParent() {
    return isParent;
  }

  public void setIsParent(Boolean isParent) {
    this.isParent = isParent;
  }

  public String getParentSummaryId() {
    return parentSummaryId;
  }

  public void setParentSummaryId(String parentSummaryId) {
    this.parentSummaryId = parentSummaryId;
  }

  public Integer getDurationInSeconds() {
    return durationInSeconds;
  }

  public void setDurationInSeconds(Integer durationInSeconds) {
    this.durationInSeconds = durationInSeconds;
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

  public String getActivityType() {
    return activityType;
  }

  public void setActivityType(String activityType) {
    this.activityType = activityType;
  }

  public Float getAverageBikeCadenceInRoundsPerMinute() {
    return averageBikeCadenceInRoundsPerMinute;
  }

  public void setAverageBikeCadenceInRoundsPerMinute(Float averageBikeCadenceInRoundsPerMinute) {
    this.averageBikeCadenceInRoundsPerMinute = averageBikeCadenceInRoundsPerMinute;
  }

  public Integer getAverageHeartRateInBeatsPerMinute() {
    return averageHeartRateInBeatsPerMinute;
  }

  public void setAverageHeartRateInBeatsPerMinute(Integer averageHeartRateInBeatsPerMinute) {
    this.averageHeartRateInBeatsPerMinute = averageHeartRateInBeatsPerMinute;
  }

  public Float getAverageRunCadenceInStepsPerMinute() {
    return averageRunCadenceInStepsPerMinute;
  }

  public void setAverageRunCadenceInStepsPerMinute(Float averageRunCadenceInStepsPerMinute) {
    this.averageRunCadenceInStepsPerMinute = averageRunCadenceInStepsPerMinute;
  }

  public Float getAverageSpeedInMetersPerSecond() {
    return averageSpeedInMetersPerSecond;
  }

  public void setAverageSpeedInMetersPerSecond(Float averageSpeedInMetersPerSecond) {
    this.averageSpeedInMetersPerSecond = averageSpeedInMetersPerSecond;
  }

  public Float getAverageSwimCadenceInStrokesPerMinute() {
    return averageSwimCadenceInStrokesPerMinute;
  }

  public void setAverageSwimCadenceInStrokesPerMinute(Float averageSwimCadenceInStrokesPerMinute) {
    this.averageSwimCadenceInStrokesPerMinute = averageSwimCadenceInStrokesPerMinute;
  }

  public Float getAveragePaceInMinutesPerKilometer() {
    return averagePaceInMinutesPerKilometer;
  }

  public void setAveragePaceInMinutesPerKilometer(Float averagePaceInMinutesPerKilometer) {
    this.averagePaceInMinutesPerKilometer = averagePaceInMinutesPerKilometer;
  }

  public Integer getActiveKilocalories() {
    return activeKilocalories;
  }

  public void setActiveKilocalories(Integer activeKilocalories) {
    this.activeKilocalories = activeKilocalories;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName;
  }

  public Float getDistanceInMeters() {
    return distanceInMeters;
  }

  public void setDistanceInMeters(Float distanceInMeters) {
    this.distanceInMeters = distanceInMeters;
  }

  public Float getMaxBikeCadenceInRoundsPerMinute() {
    return maxBikeCadenceInRoundsPerMinute;
  }

  public void setMaxBikeCadenceInRoundsPerMinute(Float maxBikeCadenceInRoundsPerMinute) {
    this.maxBikeCadenceInRoundsPerMinute = maxBikeCadenceInRoundsPerMinute;
  }

  public Integer getMaxHeartRateInBeatsPerMinute() {
    return maxHeartRateInBeatsPerMinute;
  }

  public void setMaxHeartRateInBeatsPerMinute(Integer maxHeartRateInBeatsPerMinute) {
    this.maxHeartRateInBeatsPerMinute = maxHeartRateInBeatsPerMinute;
  }

  public Float getMaxPaceInMinutesPerKilometer() {
    return maxPaceInMinutesPerKilometer;
  }

  public void setMaxPaceInMinutesPerKilometer(Float maxPaceInMinutesPerKilometer) {
    this.maxPaceInMinutesPerKilometer = maxPaceInMinutesPerKilometer;
  }

  public Float getMaxRunCadenceInStepsPerMinute() {
    return maxRunCadenceInStepsPerMinute;
  }

  public void setMaxRunCadenceInStepsPerMinute(Float maxRunCadenceInStepsPerMinute) {
    this.maxRunCadenceInStepsPerMinute = maxRunCadenceInStepsPerMinute;
  }

  public Float getMaxSpeedInMetersPerSecond() {
    return maxSpeedInMetersPerSecond;
  }

  public void setMaxSpeedInMetersPerSecond(Float maxSpeedInMetersPerSecond) {
    this.maxSpeedInMetersPerSecond = maxSpeedInMetersPerSecond;
  }

  public Integer getNumberOfActiveLengths() {
    return numberOfActiveLengths;
  }

  public void setNumberOfActiveLengths(Integer numberOfActiveLengths) {
    this.numberOfActiveLengths = numberOfActiveLengths;
  }

  public Double getStartingLatitudeInDegree() {
    return startingLatitudeInDegree;
  }

  public void setStartingLatitudeInDegree(Double startingLatitudeInDegree) {
    this.startingLatitudeInDegree = startingLatitudeInDegree;
  }

  public Double getStartingLongitudeInDegree() {
    return startingLongitudeInDegree;
  }

  public void setStartingLongitudeInDegree(Double startingLongitudeInDegree) {
    this.startingLongitudeInDegree = startingLongitudeInDegree;
  }

  public Integer getSteps() {
    return steps;
  }

  public void setSteps(Integer steps) {
    this.steps = steps;
  }

  public Float getTotalElevationGainInMeters() {
    return totalElevationGainInMeters;
  }

  public void setTotalElevationGainInMeters(Float totalElevationGainInMeters) {
    this.totalElevationGainInMeters = totalElevationGainInMeters;
  }

  public Float getTotalElevationLossInMeters() {
    return totalElevationLossInMeters;
  }

  public void setTotalElevationLossInMeters(Float totalElevationLossInMeters) {
    this.totalElevationLossInMeters = totalElevationLossInMeters;
  }

  public Boolean getManual() {
    return manual;
  }

  public void setManual(Boolean manual) {
    this.manual = manual;
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
