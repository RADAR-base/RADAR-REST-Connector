package org.radarbase.connect.rest.fitbit;

import org.radarbase.connect.rest.RestSourceConnectorConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

public class YamlFitbitUserRepository implements FitbitUserRepository {
  @Override
  public FitbitUser get(String combinedName) throws IOException {
    return null;
  }

  @Override
  public Stream<FitbitUser> stream() throws IOException {
    return null;
  }

  @Override
  public Stream<FitbitUser> stream(Stream<String> combinedNames) throws IOException {
    return null;
  }

  @Override
  public void update(FitbitUser user) throws IOException {

  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {

  }
}
