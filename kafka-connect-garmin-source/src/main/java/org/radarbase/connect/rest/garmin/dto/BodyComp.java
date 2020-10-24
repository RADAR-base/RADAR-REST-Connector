package org.radarbase.connect.rest.garmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.avro.specific.SpecificRecord;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BodyComp implements GarminData {
  private String userId;
  private String userAccessToken;
  private String summaryId;
  private Integer muscleMassInGrams;
  private Integer boneMassInGrams;
  private Float bodyWaterInPercent;
  private Float bodyFatInPercent;
  private Float bodyMassIndex;
  private Integer weightInGrams;
  private Integer measurementTimeInSeconds;
  private Integer measurementTimeOffsetInSeconds;

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

  public Integer getMuscleMassInGrams() {
    return muscleMassInGrams;
  }

  public void setMuscleMassInGrams(Integer muscleMassInGrams) {
    this.muscleMassInGrams = muscleMassInGrams;
  }

  public Integer getBoneMassInGrams() {
    return boneMassInGrams;
  }

  public void setBoneMassInGrams(Integer boneMassInGrams) {
    this.boneMassInGrams = boneMassInGrams;
  }

  public Float getBodyWaterInPercent() {
    return bodyWaterInPercent;
  }

  public void setBodyWaterInPercent(Float bodyWaterInPercent) {
    this.bodyWaterInPercent = bodyWaterInPercent;
  }

  public Float getBodyFatInPercent() {
    return bodyFatInPercent;
  }

  public void setBodyFatInPercent(Float bodyFatInPercent) {
    this.bodyFatInPercent = bodyFatInPercent;
  }

  public Float getBodyMassIndex() {
    return bodyMassIndex;
  }

  public void setBodyMassIndex(Float bodyMassIndex) {
    this.bodyMassIndex = bodyMassIndex;
  }

  public Integer getWeightInGrams() {
    return weightInGrams;
  }

  public void setWeightInGrams(Integer weightInGrams) {
    this.weightInGrams = weightInGrams;
  }

  public Integer getMeasurementTimeInSeconds() {
    return measurementTimeInSeconds;
  }

  public void setMeasurementTimeInSeconds(Integer measurementTimeInSeconds) {
    this.measurementTimeInSeconds = measurementTimeInSeconds;
  }

  public Integer getMeasurementTimeOffsetInSeconds() {
    return measurementTimeOffsetInSeconds;
  }

  public void setMeasurementTimeOffsetInSeconds(Integer measurementTimeOffsetInSeconds) {
    this.measurementTimeOffsetInSeconds = measurementTimeOffsetInSeconds;
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
