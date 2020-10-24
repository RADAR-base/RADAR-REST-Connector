package org.radarbase.connect.rest.garmin.dto;

import org.apache.avro.specific.SpecificRecord;

public class Respiration implements GarminData {

  private String userId;

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
