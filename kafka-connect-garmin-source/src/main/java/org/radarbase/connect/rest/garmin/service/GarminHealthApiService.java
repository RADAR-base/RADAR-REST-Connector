package org.radarbase.connect.rest.garmin.service;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.radarbase.connect.rest.garmin.dto.GarminData;
import org.radarbase.connect.rest.garmin.generator.DataGenerator;

@Service
public class GarminHealthApiService {

  private final DataGenerator dataGenerator;

  @Inject
  public GarminHealthApiService(DataGenerator dataGenerator) {
    this.dataGenerator = dataGenerator;
  }

  public void handleGarminData(List<? extends GarminData> garminData)
      throws IllegalStateException, IOException {
    for (GarminData data : garminData) {
      this.dataGenerator.addData(data.getUserId(), data);
    }
  }
}
