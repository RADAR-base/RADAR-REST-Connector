package org.radarbase.connect.rest.config;

import java.util.HashMap;
import org.apache.kafka.common.config.ConfigDef;

public class MethodValidator implements ConfigDef.Validator {
  @Override
  public void ensureValid(String name, Object provider) {
  }

  @Override
  public String toString() {
    return new MethodRecommender().validValues("", new HashMap<>()).toString();
  }
}
