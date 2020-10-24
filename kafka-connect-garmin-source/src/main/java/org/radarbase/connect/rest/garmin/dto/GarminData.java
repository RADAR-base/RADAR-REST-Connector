package org.radarbase.connect.rest.garmin.dto;

import org.apache.avro.specific.SpecificRecord;

public interface GarminData {
  String getUserId();

  SpecificRecord toAvroRecord();

  // TODO: Can also create converters and have a function here called getConverter() which can be
  //  then used by the SourceConnector to convert when polling but for first instance this seems
  //  fine.
}
