package org.radarbase.connect.rest.fitbit;

import org.radarbase.connect.rest.config.RestSourceTool;

import java.io.IOException;
import java.util.stream.Stream;

public interface FitbitUserRepository extends RestSourceTool {
  FitbitUser get(String combinedName) throws IOException;
  Stream<FitbitUser> stream() throws IOException;
  void update(FitbitUser user) throws IOException;
}
