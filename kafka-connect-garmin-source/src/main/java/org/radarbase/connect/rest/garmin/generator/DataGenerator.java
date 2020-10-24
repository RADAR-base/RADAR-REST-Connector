package org.radarbase.connect.rest.garmin.generator;

import java.io.IOException;
import java.util.stream.Stream;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.garmin.dto.GarminData;

/**
 * Since we use push integration, we don't generate any requests but this is used for adding the
 * data being posted and will be used by the Source Task when polled.
 */
public interface DataGenerator {

  void initialize();

  void addData(String userId, GarminData data) throws IllegalStateException, IOException;

  Stream<? extends GarminData> getAllPendingData(int next) throws IOException;

  Stream<? extends GarminData> getAllPendingDataForUser(String userId, int next) throws IOException;

  void stop();
}
