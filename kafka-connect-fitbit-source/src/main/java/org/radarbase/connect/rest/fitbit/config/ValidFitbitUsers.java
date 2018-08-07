package org.radarbase.connect.rest.fitbit.config;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.List;

import static org.radarbase.connect.rest.RestSourceConnectorConfig.COLON_PATTERN;

public class ValidFitbitUsers implements ConfigDef.Validator {
  @Override
  public void ensureValid(String name, Object value) {
    if (value instanceof String) {
      ensureValidString(name, (String) value);
    }
    if (value instanceof List) {
      ((List<?>)value).forEach(v -> ensureValidString(name, (String)v));
    }
  }

  private void ensureValidString(String name, String s) {
    if (s == null || s.isEmpty()) {
      return;
    }
    String[] split = COLON_PATTERN.split(s);
    if (split.length != 6) {
      throw new ConfigException(name, s,
          "Fitbit user syntax should be"
              + " fitbitUserName:refreshToken:projectId:userName:startDate:endDate");
    }
    if (split[0].isEmpty() || split[1].isEmpty() || split[2].isEmpty() || split[3].isEmpty()) {
      throw new ConfigException(name, s,
          "Fitbit user properties user name, refresh token, project ID and user name"
              + " may not be empty.");
    }
  }
}
