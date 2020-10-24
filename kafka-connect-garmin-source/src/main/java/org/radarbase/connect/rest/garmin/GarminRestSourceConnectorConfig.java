package org.radarbase.connect.rest.garmin;

import java.util.Collections;
import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Validator;
import org.apache.kafka.common.config.ConfigDef.Width;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.garmin.generator.DataGenerator;
import org.radarbase.connect.rest.garmin.generator.GarminDataGenerator;
import org.radarbase.connect.rest.garmin.user.ServiceUserRepository;
import org.radarbase.connect.rest.garmin.user.UserRepository;

public class GarminRestSourceConnectorConfig extends RestSourceConnectorConfig {
  public static final String GARMIN_USERS_CONFIG = "garmin.users";
  private static final String GARMIN_USERS_DOC =
      "The user ID of Garmin users to include in polling, separated by commas. "
          + "Non existing user names will be ignored. "
          + "If empty, all users in the user directory will be used.";
  private static final String GARMIN_USERS_DISPLAY = "Garmin users";

  private UserRepository userRepository = new ServiceUserRepository();
  private DataGenerator dataGenerator = new GarminDataGenerator(userRepository);

  public GarminRestSourceConnectorConfig(
      ConfigDef config, Map<String, String> parsedConfig, boolean doLog) {
    super(config, parsedConfig, doLog);
  }

  public GarminRestSourceConnectorConfig(Map<String, String> parsedConfig, boolean doLog) {
    super(parsedConfig, doLog);
  }

  public static ConfigDef conf() {
    int orderInGroup = 0;
    String group = "Fitbit";

    Validator nonControlChar =
        new ConfigDef.NonEmptyStringWithoutControlChars() {
          @Override
          public String toString() {
            return "non-empty string without control characters";
          }
        };

    return RestSourceConnectorConfig.conf()
        .define(
            GARMIN_USERS_CONFIG,
            Type.LIST,
            Collections.emptyList(),
            Importance.HIGH,
            GARMIN_USERS_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            GARMIN_USERS_DISPLAY);
  }

  public DataGenerator getDataGenerator() {
    return dataGenerator;
  }

  public UserRepository getUserRepository(UserRepository userRepository) {
    return userRepository;
  }
}
