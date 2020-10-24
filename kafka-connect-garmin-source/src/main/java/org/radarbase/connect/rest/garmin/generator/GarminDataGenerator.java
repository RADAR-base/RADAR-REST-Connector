package org.radarbase.connect.rest.garmin.generator;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.Stream;
import org.radarbase.connect.rest.garmin.dto.GarminData;
import org.radarbase.connect.rest.garmin.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GarminDataGenerator implements DataGenerator {

  private static final Logger logger = LoggerFactory.getLogger(GarminDataGenerator.class);
  private final Map<String, SynchronousQueue<GarminData>> data;
  private boolean isRunning;
  private final UserRepository userRepository;

  public GarminDataGenerator(UserRepository userRepository) {
    this.data = new ConcurrentHashMap<>();
    this.userRepository = userRepository;
  }

  @Override
  public void initialize() {
    isRunning = true;
  }

  @Override
  public void addData(String userId, GarminData userData) {
    if (!isRunning) {
      throw new IllegalStateException("Please Initialise before adding data");
    }

    // TODO: put users using versionedID instead of the externalId. Can lookup the user in the
    //  userRepository using the external userId,
    //  Ignore data if user does not exist in the user repository

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
    logger.info("Added data {} for user: {}", userData.getClass().getSimpleName(), userId);
  }

  @Override
  public Stream<? extends GarminData> getAllPendingData(int maxElements) {
    return data.values().stream().flatMap(Queue::stream).limit(maxElements).sorted();
  }

  @Override
  public Stream<? extends GarminData> getAllPendingDataForUser(String userId, int maxElements) {
    if (data.containsKey(userId)) {
      Queue<GarminData> queue = data.get(userId);
      if (queue.size() < maxElements) {
        maxElements = queue.size();
      }
      return Stream.generate(data.get(userId)::poll).limit(maxElements).sorted();
    }
    return Stream.empty();
  }

  @Override
  public void stop() {
    isRunning = false;
  }
}
