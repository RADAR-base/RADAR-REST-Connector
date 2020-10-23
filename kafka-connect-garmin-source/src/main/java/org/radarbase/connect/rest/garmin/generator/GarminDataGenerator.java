package org.radarbase.connect.rest.garmin.generator;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.Stream;
import org.radarbase.connect.rest.garmin.dto.GarminData;

public class GarminDataGenerator implements DataGenerator {

  private final Map<String, SynchronousQueue<GarminData>> data;

  public GarminDataGenerator() {
    data = new ConcurrentHashMap<>();
  }

  @Override
  public void addData(String userId, GarminData userData) {
    SynchronousQueue<GarminData> queue;
    if (data.containsKey(userId)) {
      queue = data.get(userId);
      if (queue == null) {
        queue = new SynchronousQueue<>();
      }
    } else {
      queue = new SynchronousQueue<>();
    }
    queue.offer(userData);
    data.put(userId, queue);
  }

  @Override
  public Stream<? extends GarminData> getAllPendingData(int maxElements) {
    return data.values().stream().flatMap(Queue::stream).limit(maxElements).sorted();
  }

  @Override
  public Stream<? extends GarminData> getAllPendingDataForUser(String userId, int maxElements) {
    if (data.containsKey(userId)) {
      return Stream.generate(data.get(userId)::poll).limit(maxElements).sorted();
    }
    return Stream.empty();
  }
}
