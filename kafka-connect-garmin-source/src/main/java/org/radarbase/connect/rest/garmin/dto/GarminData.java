package org.radarbase.connect.rest.garmin.dto;

import org.apache.avro.specific.SpecificRecord;

public interface GarminData {
  SpecificRecord toAvroRecord();

  // TODO: Can also create converters and have a function here called getConverter() which can be
  //  then used by the SourceConnector to convert when polling
}
