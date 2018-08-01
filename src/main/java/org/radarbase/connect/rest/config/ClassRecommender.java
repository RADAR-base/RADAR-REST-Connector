package org.radarbase.connect.rest.config;

import org.apache.kafka.common.config.ConfigDef;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClassRecommender implements ConfigDef.Recommender {
  private final List<Object> classes;

  public static ClassRecommender implementations(Class<?>... classes) {
    return new ClassRecommender(Arrays.asList(classes));
  }

  private ClassRecommender(List<Object> classes) {
    this.classes = classes;
  }
  @Override
  public List<Object> validValues(String name, Map<String, Object> parsedConfig) {
    return classes;
  }

  @Override
  public boolean visible(String name, Map<String, Object> parsedConfig) {
    return false;
  }
}
