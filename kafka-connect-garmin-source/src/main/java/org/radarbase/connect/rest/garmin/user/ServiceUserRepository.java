package org.radarbase.connect.rest.garmin.user;

import java.io.IOException;
import java.util.stream.Stream;
import javax.ws.rs.NotAuthorizedException;
import org.radarbase.connect.rest.RestSourceConnectorConfig;

public class ServiceUserRepository implements UserRepository{

  @Override
  public User get(String key) throws IOException {
    return null;
  }

  @Override
  public Stream<? extends User> stream() throws IOException {
    return null;
  }

  @Override
  public String getAccessToken(User user) throws IOException, NotAuthorizedException {
    return null;
  }

  @Override
  public String getUserAccessTokenSecret(User user) throws IOException, NotAuthorizedException {
    return null;
  }

  @Override
  public boolean hasPendingUpdates() {
    return false;
  }

  @Override
  public void applyPendingUpdates() throws IOException {

  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {

  }
}
