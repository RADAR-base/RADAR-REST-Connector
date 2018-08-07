package org.radarbase.connect.rest.fitbit.user;

import org.radarbase.connect.rest.config.RestSourceTool;

import java.io.IOException;
import java.util.stream.Stream;

public interface FitbitUserRepository extends RestSourceTool {
  FitbitUser get(String key) throws IOException;
  Stream<FitbitUser> stream() throws IOException;
  void update(FitbitUser user) throws IOException;
}
