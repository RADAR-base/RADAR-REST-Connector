package org.radarbase.connect.rest.garmin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.source.SourceTaskContext;

public class GarminSourceTask extends SourceTask {

  private List<String> users;

  @Override
  public void initialize(SourceTaskContext context) {
    super.initialize(context);
    users = Arrays.asList(context.configs().get("garmin.users").split(",").clone());
  }

  @Override
  public String version() {
    return null;
  }

  @Override
  public void start(Map<String, String> props) {
    // configure
    // get list of users to poll

  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    // get next `records.poll.size` number of data from the data generator
    // process the records
    return null;
  }

  @Override
  public void stop() {
    // stop the data generator (so that no more data can be added)
    // get all pending data from data generator and process the records
  }
}
