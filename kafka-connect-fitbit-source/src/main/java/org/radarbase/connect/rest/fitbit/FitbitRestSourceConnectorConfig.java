/*
 * Copyright 2018 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.radarbase.connect.rest.fitbit;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import static org.apache.kafka.common.config.ConfigDef.NO_DEFAULT_VALUE;
import org.apache.kafka.common.config.ConfigDef.NonEmptyString;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Validator;
import org.apache.kafka.common.config.ConfigDef.Width;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.config.ValidClass;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.connect.rest.fitbit.user.YamlUserRepository;

import okhttp3.Headers;
import okhttp3.HttpUrl;

public class FitbitRestSourceConnectorConfig extends RestSourceConnectorConfig {
  public static final String FITBIT_USERS_CONFIG = "fitbit.users";
  private static final String FITBIT_USERS_DOC =
      "The user ID of Fitbit users to include in polling, separated by commas. "
          + "Non existing user names will be ignored. "
          + "If empty, all users in the user directory will be used.";
  private static final String FITBIT_USERS_DISPLAY = "Fitbit users";

  public static final String FITBIT_API_CLIENT_CONFIG = "fitbit.api.client";
  private static final String FITBIT_API_CLIENT_DOC =
      "Client ID for the Fitbit API";
  private static final String FITBIT_API_CLIENT_DISPLAY = "Fitbit API client ID";

  public static final String FITBIT_API_SECRET_CONFIG = "fitbit.api.secret";
  private static final String FITBIT_API_SECRET_DOC = "Secret for the Fitbit API client set in fitbit.api.client.";
  private static final String FITBIT_API_SECRET_DISPLAY = "Fitbit API client secret";

  public static final String FITBIT_USER_REPOSITORY_CONFIG = "fitbit.user.repository.class";
  private static final String FITBIT_USER_REPOSITORY_DOC = "Class for managing users and authentication.";
  private static final String FITBIT_USER_REPOSITORY_DISPLAY = "User repository class";

  public static final String FITBIT_API_INTRADAY_ACCESS_CONFIG = "fitbit.api.intraday";
  private static final String FITBIT_API_INTRADAY_ACCESS_DOC = "Set to true if the client has permissions to Fitbit Intraday API, false otherwise.";
  private static final boolean FITBIT_API_INTRADAY_ACCESS_DEFAULT = false;
  private static final String FITBIT_API_INTRADAY_ACCESS_DISPLAY = "Is Fitbit Intraday API available?";

  public static final String FITBIT_USER_POLL_INTERVAL = "fitbit.user.poll.interval";
  private static final String FITBIT_USER_POLL_INTERVAL_DOC = "Polling interval per Fitbit user per request route in seconds.";
  // 150 requests per hour -> 2.5 per minute. There are currently 5 paths, that limits us to 1
  // call per route per 2 minutes.
  private static final int FITBIT_USER_POLL_INTERVAL_DEFAULT = 150;
  private static final String FITBIT_USER_POLL_INTERVAL_DISPLAY = "Per-user per-route polling interval.";


  public static final String FITBIT_USER_CREDENTIALS_DIR_CONFIG = "fitbit.user.dir";
  private static final String FITBIT_USER_CREDENTIALS_DIR_DOC = "Directory containing Fitbit user information and credentials. Only used if a file-based user repository is configured.";
  private static final String FITBIT_USER_CREDENTIALS_DIR_DISPLAY = "User directory";
  private static final String FITBIT_USER_CREDENTIALS_DIR_DEFAULT = "/var/lib/kafka-connect-fitbit-source/users";

  public static final String FITBIT_USER_REPOSITORY_URL_CONFIG = "fitbit.user.repository.url";
  private static final String FITBIT_USER_REPOSITORY_URL_DOC = "URL for webservice containing user credentials. Only used if a webservice-based user repository is configured.";
  private static final String FITBIT_USER_REPOSITORY_URL_DISPLAY = "User repository URL";
  private static final String FITBIT_USER_REPOSITORY_URL_DEFAULT = "";

  private static final String FITBIT_INTRADAY_STEPS_TOPIC_CONFIG = "fitbit.intraday.steps.topic";
  private static final String FITBIT_INTRADAY_STEPS_TOPIC_DOC = "Topic for Fitbit intraday steps";
  private static final String FITBIT_INTRADAY_STEPS_TOPIC_DISPLAY = "Intraday steps topic";
  private static final String FITBIT_INTRADAY_STEPS_TOPIC_DEFAULT = "connect_fitbit_intraday_steps";

  private static final String FITBIT_INTRADAY_STEPS_ENABLED_CONFIG = "fitbit.intraday.steps.enabled";
  private static final String FITBIT_INTRADAY_STEPS_ENABLED_DOC = "Enable or disable intraday steps";
  private static final String FITBIT_INTRADAY_STEPS_ENABLED_DISPLAY = "Intraday steps enabled";
  private static final boolean FITBIT_INTRADAY_STEPS_ENABLED_DEFAULT = true;


  private static final String FITBIT_INTRADAY_HEART_RATE_TOPIC_CONFIG = "fitbit.intraday.heart.rate.topic";
  private static final String FITBIT_INTRADAY_HEART_RATE_TOPIC_DOC = "Topic for Fitbit intraday heart_rate";
  private static final String FITBIT_INTRADAY_HEART_RATE_TOPIC_DISPLAY = "Intraday heartrate topic";
  private static final String FITBIT_INTRADAY_HEART_RATE_TOPIC_DEFAULT = "connect_fitbit_intraday_heart_rate";

  private static final String FITBIT_INTRADAY_HEART_RATE_ENABLED_CONFIG = "fitbit.intraday.heart.rate.enabled";
  private static final String FITBIT_INTRADAY_HEART_RATE_ENABLED_DOC = "Enable or disable intraday heart rate";
  private static final String FITBIT_INTRADAY_HEART_RATE_ENABLED_DISPLAY = "Intraday heart rate enabled";
  private static final boolean FITBIT_INTRADAY_HEART_RATE_ENABLED_DEFAULT = true;
  

  private static final String FITBIT_INTRADAY_HEART_RATE_VARIABILITY_TOPIC_CONFIG = "fitbit.intraday.heart.rate.variability.topic";
  private static final String FITBIT_INTRADAY_HEART_RATE_VARIABILITY_TOPIC_DOC = "Topic for Fitbit intraday intraday_heart_rate_variability";
  private static final String FITBIT_INTRADAY_HEART_RATE_VARIABILITY_TOPIC_DISPLAY = "Intraday heart rate variability topic";
  private static final String FITBIT_INTRADAY_HEART_RATE_VARIABILITY_TOPIC_DEFAULT = "connect_fitbit_intraday_heart_rate_variability";

  private static final String FITBIT_INTRADAY_HEART_RATE_VARIABILITY_ENABLED_CONFIG = "fitbit.intraday.heart.rate.variability.enabled";
  private static final String FITBIT_INTRADAY_HEART_RATE_VARIABILITY_ENABLED_DOC = "Enable or disable intraday heart rate variability";
  private static final String FITBIT_INTRADAY_HEART_RATE_VARIABILITY_ENABLED_DISPLAY = "Intraday heart rate variability enabled";
  private static final boolean FITBIT_INTRADAY_HEART_RATE_VARIABILITY_ENABLED_DEFAULT = true;


  private static final String FITBIT_INTRADAY_SPO2_TOPIC_CONFIG = "fitbit.intraday.spo2.topic";
  private static final String FITBIT_INTRADAY_SPO2_TOPIC_DOC = "Topic for Fitbit intraday intraday_spo2";
  private static final String FITBIT_INTRADAY_SPO2_TOPIC_DISPLAY = "Intraday spo2 topic";
  private static final String FITBIT_INTRADAY_SPO2_TOPIC_DEFAULT = "connect_fitbit_intraday_spo2";

  private static final String FITBIT_INTRADAY_SPO2_ENABLED_CONFIG = "fitbit.intraday.spo2.enabled";
  private static final String FITBIT_INTRADAY_SPO2_ENABLED_DOC = "Enable or disable intraday spo2";
  private static final String FITBIT_INTRADAY_SPO2_ENABLED_DISPLAY = "Intraday spo2 enabled";
  private static final boolean FITBIT_INTRADAY_SPO2_ENABLED_DEFAULT = true;


  private static final String FITBIT_BREATHING_RATE_TOPIC_CONFIG = "fitbit.breathing.rate.topic";
  private static final String FITBIT_BREATHING_RATE_TOPIC_DOC = "Topic for Fitbit breathing rate";
  private static final String FITBIT_BREATHING_RATE_TOPIC_DISPLAY = "Breathing rate topic";
  private static final String FITBIT_BREATHING_RATE_TOPIC_DEFAULT = "connect_fitbit_breathing_rate";

  private static final String FITBIT_BREATHING_RATE_ENABLED_CONFIG = "fitbit.breathing.rate.enabled";
  private static final String FITBIT_BREATHING_RATE_ENABLED_DOC = "Enable or disable breathing rate";
  private static final String FITBIT_BREATHING_RATE_ENABLED_DISPLAY = "Breathing rate enabled";
  private static final boolean FITBIT_BREATHING_RATE_ENABLED_DEFAULT = true;


  private static final String FITBIT_SKIN_TEMPERATURE_TOPIC_CONFIG = "fitbit.skin.temperature.rate.topic";
  private static final String FITBIT_SKIN_TEMPERATURE_TOPIC_DOC = "Topic for Fitbit skin temperature";
  private static final String FITBIT_SKIN_TEMPERATURE_TOPIC_DISPLAY = "Skin temperature topic";
  private static final String FITBIT_SKIN_TEMPERATURE_TOPIC_DEFAULT = "connect_fitbit_skin_temperature";

  private static final String FITBIT_SKIN_TEMPERATURE_ENABLED_CONFIG = "fitbit.skin.temperature.enabled";
  private static final String FITBIT_SKIN_TEMPERATURE_ENABLED_DOC = "Enable or disable skin temperature";
  private static final String FITBIT_SKIN_TEMPERATURE_ENABLED_DISPLAY = "Skin temperature enabled";
  private static final boolean FITBIT_SKIN_TEMPERATURE_ENABLED_DEFAULT = true;


  private static final String FITBIT_RESTING_HEART_RATE_TOPIC_CONFIG = "fitbit.resting.heart.rate.topic";
  private static final String FITBIT_RESTING_HEART_RATE_TOPIC_DOC = "Topic for Fitbit resting heart_rate";
  private static final String FITBIT_RESTING_HEART_RATE_TOPIC_DISPLAY = "Resting heartrate topic";
  private static final String FITBIT_RESTING_HEART_RATE_TOPIC_DEFAULT = "connect_fitbit_resting_heart_rate";

  private static final String FITBIT_RESTING_HEART_RATE_ENABLED_CONFIG = "fitbit.resting.heart.rate.enabled";
  private static final String FITBIT_RESTING_HEART_RATE_ENABLED_DOC = "Enable or disable resting heart rate";
  private static final String FITBIT_RESTING_HEART_RATE_ENABLED_DISPLAY = "Resting heart rate enabled";
  private static final boolean FITBIT_RESTING_HEART_RATE_ENABLED_DEFAULT = true;


  private static final String FITBIT_SLEEP_STAGES_TOPIC_CONFIG = "fitbit.sleep.stages.topic";
  private static final String FITBIT_SLEEP_STAGES_TOPIC_DOC = "Topic for Fitbit sleep stages";
  private static final String FITBIT_SLEEP_STAGES_TOPIC_DEFAULT = "connect_fitbit_sleep_stages";
  private static final String FITBIT_SLEEP_STAGES_TOPIC_DISPLAY = "Sleep stages topic";

  private static final String FITBIT_SLEEP_STAGES_ENABLED_CONFIG = "fitbit.sleep.stages.enabled";
  private static final String FITBIT_SLEEP_STAGES_ENABLED_DOC = "Enable or disable sleep stages";
  private static final String FITBIT_SLEEP_STAGES_ENABLED_DISPLAY = "Sleep stages enabled";
  private static final boolean FITBIT_SLEEP_STAGES_ENABLED_DEFAULT = true;


  private static final String FITBIT_SLEEP_CLASSIC_TOPIC_CONFIG = "fitbit.sleep.classic.topic";
  private static final String FITBIT_SLEEP_CLASSIC_TOPIC_DOC = "Topic for Fitbit sleep classic data";
  private static final String FITBIT_SLEEP_CLASSIC_TOPIC_DEFAULT = "connect_fitbit_sleep_classic";
  private static final String FITBIT_SLEEP_CLASSIC_TOPIC_DISPLAY = "Classic sleep topic";

  private static final String FITBIT_SLEEP_CLASSIC_ENABLED_CONFIG = "fitbit.sleep.classic.enabled";
  private static final String FITBIT_SLEEP_CLASSIC_ENABLED_DOC = "Enable or disable sleep classic";
  private static final String FITBIT_SLEEP_CLASSIC_ENABLED_DISPLAY = "Sleep classic enabled";
  private static final boolean FITBIT_SLEEP_CLASSIC_ENABLED_DEFAULT = true;


  private static final String FITBIT_TIME_ZONE_TOPIC_CONFIG = "fitbit.time.zone.topic";
  private static final String FITBIT_TIME_ZONE_TOPIC_DOC = "Topic for Fitbit profile time zone";
  private static final String FITBIT_TIME_ZONE_TOPIC_DEFAULT = "connect_fitbit_time_zone";
  private static final String FITBIT_TIME_ZONE_TOPIC_DISPLAY = "Time zone topic";

  private static final String FITBIT_TIME_ZONE_ENABLED_CONFIG = "fitbit.time.zone.enabled";
  private static final String FITBIT_TIME_ZONE_ENABLED_DOC = "Enable or disable time zone";
  private static final String FITBIT_TIME_ZONE_ENABLED_DISPLAY = "Time zone enabled";
  private static final boolean FITBIT_TIME_ZONE_ENABLED_DEFAULT = true;
  

  private static final String FITBIT_ACTIVITY_LOG_TOPIC_CONFIG = "fitbit.activity.log.topic";
  private static final String FITBIT_ACTIVITY_LOG_TOPIC_DOC = "Topic for Fitbit activity log.";
  private static final String FITBIT_ACTIVITY_LOG_TOPIC_DEFAULT = "connect_fitbit_activity_log";
  private static final String FITBIT_ACTIVITY_LOG_TOPIC_DISPLAY = "Activity log topic";

  private static final String FITBIT_ACTIVITY_LOG_ENABLED_CONFIG = "fitbit.activity.log.enabled";
  private static final String FITBIT_ACTIVITY_LOG_ENABLED_DOC = "Enable or disable activity log";
  private static final String FITBIT_ACTIVITY_LOG_ENABLED_DISPLAY = "Activity log enabled";
  private static final boolean FITBIT_ACTIVITY_LOG_ENABLED_DEFAULT = true;


  private static final String FITBIT_INTRADAY_CALORIES_TOPIC_CONFIG = "fitbit.intraday.calories.topic";
  private static final String FITBIT_INTRADAY_CALORIES_TOPIC_DOC = "Topic for Fitbit intraday calories";
  private static final String FITBIT_INTRADAY_CALORIES_TOPIC_DISPLAY = "Intraday calories topic";
  private static final String FITBIT_INTRADAY_CALORIES_TOPIC_DEFAULT = "connect_fitbit_intraday_calories";

  private static final String FITBIT_INTRADAY_CALORIES_ENABLED_CONFIG = "fitbit.intraday.calories.enabled";
  private static final String FITBIT_INTRADAY_CALORIES_ENABLED_DOC = "Enable or disable intraday calories";
  private static final String FITBIT_INTRADAY_CALORIES_ENABLED_DISPLAY = "Intraday calories enabled";
  private static final boolean FITBIT_INTRADAY_CALORIES_ENABLED_DEFAULT = true;


  public static final String FITBIT_USER_REPOSITORY_CLIENT_ID_CONFIG = "fitbit.user.repository.client.id";
  private static final String FITBIT_USER_REPOSITORY_CLIENT_ID_DOC = "Client ID for connecting to the service repository.";
  private static final String FITBIT_USER_REPOSITORY_CLIENT_ID_DISPLAY = "Client ID for user repository.";

  public static final String FITBIT_USER_REPOSITORY_CLIENT_SECRET_CONFIG = "fitbit.user.repository.client.secret";
  private static final String FITBIT_USER_REPOSITORY_CLIENT_SECRET_DOC = "Client secret for connecting to the service repository.";
  private static final String FITBIT_USER_REPOSITORY_CLIENT_SECRET_DISPLAY = "Client Secret for user repository.";

  public static final String FITBIT_USER_REPOSITORY_TOKEN_URL_CONFIG = "fitbit.user.repository.oauth2.token.url";
  private static final String FITBIT_USER_REPOSITORY_TOKEN_URL_DOC = "OAuth 2.0 token url for retrieving client credentials.";
  private static final String FITBIT_USER_REPOSITORY_TOKEN_URL_DISPLAY = "OAuth 2.0 token URL.";

  public static final String FITBIT_USER_REPOSITORY_FIRESTORE_FITBIT_COLLECTION_CONFIG = "fitbit.user.firebase.collection.fitbit.name";
  private static final String FITBIT_USER_REPOSITORY_FIRESTORE_FITBIT_COLLECTION_DOC = "Firestore Collection for retrieving Fitbit Auth details. Only used when a Firebase based user repository is used.";
  private static final String FITBIT_USER_REPOSITORY_FIRESTORE_FITBIT_COLLECTION_DISPLAY = "Firebase Fitbit collection name.";
  private static final String FITBIT_USER_REPOSITORY_FIRESTORE_FITBIT_COLLECTION_DEFAULT = "fitbit";

  public static final String FITBIT_USER_REPOSITORY_FIRESTORE_USER_COLLECTION_CONFIG = "fitbit.user.firebase.collection.user.name";
  private static final String FITBIT_USER_REPOSITORY_FIRESTORE_USER_COLLECTION_DOC = "Firestore Collection for retrieving User details. Only used when a Firebase based user repository is used.";
  private static final String FITBIT_USER_REPOSITORY_FIRESTORE_USER_COLLECTION_DISPLAY = "Firebase User collection name.";
  private static final String FITBIT_USER_REPOSITORY_FIRESTORE_USER_COLLECTION_DEFAULT = "users";

  private static final String FITBIT_MAX_FORBIDDEN_CONFIG = "fitbit.request.max.forbidden";
  private static final String FITBIT_MAX_FORBIDDEN_DOC = "Maximum number of consecutive forbidden responses before backing off.";
  private static final String FITBIT_MAX_FORBIDDEN_DISPLAY = "Max forbidden responses";
  private static final int FITBIT_MAX_FORBIDDEN_DEFAULT = 3;

  private static final String FITBIT_FORBIDDEN_BACKOFF_CONFIG = "fitbit.request.forbidden.backoff.s";
  private static final String FITBIT_FORBIDDEN_BACKOFF_DOC = "Backoff time in seconds between forbidden requests.";
  private static final String FITBIT_FORBIDDEN_BACKOFF_DISPLAY = "Forbidden backoff time (s)";
  private static final int FITBIT_FORBIDDEN_BACKOFF_DEFAULT = 86400; // 24 hours


  private UserRepository userRepository;
  private final Headers clientCredentials;

  public FitbitRestSourceConnectorConfig(ConfigDef config, Map<String, String> parsedConfig, boolean doLog) {
    super(config, parsedConfig, doLog);

    String credentialString = getFitbitClient() + ":" + getFitbitClientSecret();
    String credentialsBase64 = Base64.getEncoder().encodeToString(
        credentialString.getBytes(StandardCharsets.UTF_8));
    this.clientCredentials = Headers.of("Authorization", "Basic " + credentialsBase64);
  }

  public FitbitRestSourceConnectorConfig(Map<String, String> parsedConfig, boolean doLog) {
    this(FitbitRestSourceConnectorConfig.conf(), parsedConfig, doLog);
  }

  public static ConfigDef conf() {
    int orderInGroup = 0;
    String group = "Fitbit";

    Validator nonControlChar = new ConfigDef.NonEmptyStringWithoutControlChars() {
      @Override
      public String toString() {
        return "non-empty string without control characters";
      }
    };

    return RestSourceConnectorConfig.conf()
        .define(FITBIT_USERS_CONFIG,
            Type.LIST,
            Collections.emptyList(),
            Importance.HIGH,
            FITBIT_USERS_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USERS_DISPLAY)

        .define(FITBIT_API_CLIENT_CONFIG,
            Type.STRING,
            NO_DEFAULT_VALUE,
            new NonEmptyString(),
            Importance.HIGH,
            FITBIT_API_CLIENT_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_API_CLIENT_DISPLAY)

        .define(FITBIT_API_SECRET_CONFIG,
            Type.PASSWORD,
            NO_DEFAULT_VALUE,
            Importance.HIGH,
            FITBIT_API_SECRET_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_API_SECRET_DISPLAY)

        .define(FITBIT_USER_POLL_INTERVAL,
            Type.INT,
            FITBIT_USER_POLL_INTERVAL_DEFAULT,
            Importance.MEDIUM,
            FITBIT_USER_POLL_INTERVAL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USER_POLL_INTERVAL_DISPLAY)

        .define(FITBIT_API_INTRADAY_ACCESS_CONFIG,
            Type.BOOLEAN,
            FITBIT_API_INTRADAY_ACCESS_DEFAULT,
            Importance.MEDIUM,
            FITBIT_API_INTRADAY_ACCESS_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_API_INTRADAY_ACCESS_DISPLAY)

        .define(FITBIT_USER_REPOSITORY_CONFIG,
            Type.CLASS,
            YamlUserRepository.class,
            ValidClass.isSubclassOf(UserRepository.class),
            Importance.MEDIUM,
            FITBIT_USER_REPOSITORY_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USER_REPOSITORY_DISPLAY)

        .define(FITBIT_USER_CREDENTIALS_DIR_CONFIG,
            Type.STRING,
            FITBIT_USER_CREDENTIALS_DIR_DEFAULT,
            Importance.LOW,
            FITBIT_USER_CREDENTIALS_DIR_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USER_CREDENTIALS_DIR_DISPLAY)

        .define(FITBIT_USER_REPOSITORY_URL_CONFIG,
            Type.STRING,
            FITBIT_USER_REPOSITORY_URL_DEFAULT,
            Importance.LOW,
            FITBIT_USER_REPOSITORY_URL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USER_REPOSITORY_URL_DISPLAY)


        .define(FITBIT_USER_REPOSITORY_CLIENT_ID_CONFIG,
            Type.STRING,
            "",
            Importance.MEDIUM,
            FITBIT_USER_REPOSITORY_CLIENT_ID_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USER_REPOSITORY_CLIENT_ID_DISPLAY)

        .define(FITBIT_USER_REPOSITORY_CLIENT_SECRET_CONFIG,
            Type.PASSWORD,
            "",
            Importance.MEDIUM,
            FITBIT_USER_REPOSITORY_CLIENT_SECRET_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USER_REPOSITORY_CLIENT_SECRET_DISPLAY)

        .define(FITBIT_USER_REPOSITORY_TOKEN_URL_CONFIG,
            Type.STRING,
            "",
            Importance.MEDIUM,
            FITBIT_USER_REPOSITORY_TOKEN_URL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USER_REPOSITORY_TOKEN_URL_DISPLAY)

        .define(FITBIT_INTRADAY_STEPS_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_INTRADAY_STEPS_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_INTRADAY_STEPS_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_STEPS_TOPIC_DISPLAY)

        .define(FITBIT_INTRADAY_STEPS_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_INTRADAY_STEPS_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_INTRADAY_STEPS_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_STEPS_ENABLED_DISPLAY)

        .define(FITBIT_INTRADAY_HEART_RATE_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_INTRADAY_HEART_RATE_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_INTRADAY_HEART_RATE_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_HEART_RATE_TOPIC_DISPLAY)
        

        .define(FITBIT_INTRADAY_HEART_RATE_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_INTRADAY_HEART_RATE_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_INTRADAY_HEART_RATE_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_HEART_RATE_ENABLED_DISPLAY)

        .define(FITBIT_INTRADAY_HEART_RATE_VARIABILITY_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_INTRADAY_HEART_RATE_VARIABILITY_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_INTRADAY_HEART_RATE_VARIABILITY_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_HEART_RATE_VARIABILITY_TOPIC_DISPLAY)

        .define(FITBIT_INTRADAY_HEART_RATE_VARIABILITY_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_INTRADAY_HEART_RATE_VARIABILITY_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_INTRADAY_HEART_RATE_VARIABILITY_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_HEART_RATE_VARIABILITY_ENABLED_DISPLAY)


        .define(FITBIT_INTRADAY_SPO2_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_INTRADAY_SPO2_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_INTRADAY_SPO2_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_SPO2_TOPIC_DISPLAY)

        .define(FITBIT_INTRADAY_SPO2_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_INTRADAY_SPO2_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_INTRADAY_SPO2_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_SPO2_ENABLED_DISPLAY)

        .define(FITBIT_BREATHING_RATE_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_BREATHING_RATE_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_BREATHING_RATE_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_BREATHING_RATE_TOPIC_DISPLAY)

        .define(FITBIT_BREATHING_RATE_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_BREATHING_RATE_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_BREATHING_RATE_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_BREATHING_RATE_ENABLED_DISPLAY)

        .define(FITBIT_SKIN_TEMPERATURE_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_SKIN_TEMPERATURE_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_SKIN_TEMPERATURE_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_SKIN_TEMPERATURE_TOPIC_DISPLAY)

        .define(FITBIT_SKIN_TEMPERATURE_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_SKIN_TEMPERATURE_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_SKIN_TEMPERATURE_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_SKIN_TEMPERATURE_ENABLED_DISPLAY)

        .define(FITBIT_RESTING_HEART_RATE_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_RESTING_HEART_RATE_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_RESTING_HEART_RATE_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_RESTING_HEART_RATE_TOPIC_DISPLAY)

        .define(FITBIT_RESTING_HEART_RATE_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_RESTING_HEART_RATE_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_RESTING_HEART_RATE_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_RESTING_HEART_RATE_ENABLED_DISPLAY)

        .define(FITBIT_SLEEP_STAGES_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_SLEEP_STAGES_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_SLEEP_STAGES_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_SLEEP_STAGES_TOPIC_DISPLAY)

        .define(FITBIT_SLEEP_STAGES_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_SLEEP_STAGES_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_SLEEP_STAGES_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_SLEEP_STAGES_ENABLED_DISPLAY)

        .define(FITBIT_SLEEP_CLASSIC_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_SLEEP_CLASSIC_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_SLEEP_CLASSIC_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_SLEEP_CLASSIC_TOPIC_DISPLAY)
        
        .define(FITBIT_SLEEP_CLASSIC_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_SLEEP_CLASSIC_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_SLEEP_CLASSIC_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_SLEEP_CLASSIC_ENABLED_DISPLAY)

        .define(FITBIT_TIME_ZONE_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_TIME_ZONE_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_TIME_ZONE_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_TIME_ZONE_TOPIC_DISPLAY)

        .define(FITBIT_TIME_ZONE_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_TIME_ZONE_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_TIME_ZONE_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_TIME_ZONE_ENABLED_DISPLAY)

        .define(FITBIT_ACTIVITY_LOG_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_ACTIVITY_LOG_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_ACTIVITY_LOG_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_ACTIVITY_LOG_TOPIC_DISPLAY)

        .define(FITBIT_ACTIVITY_LOG_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_ACTIVITY_LOG_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_ACTIVITY_LOG_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_ACTIVITY_LOG_ENABLED_DISPLAY)

        .define(FITBIT_INTRADAY_CALORIES_TOPIC_CONFIG,
            Type.STRING,
            FITBIT_INTRADAY_CALORIES_TOPIC_DEFAULT,
            nonControlChar,
            Importance.LOW,
            FITBIT_INTRADAY_CALORIES_TOPIC_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_CALORIES_TOPIC_DISPLAY)

        .define(FITBIT_INTRADAY_CALORIES_ENABLED_CONFIG,
            Type.BOOLEAN,
            FITBIT_INTRADAY_CALORIES_ENABLED_DEFAULT,
            Importance.LOW,
            FITBIT_INTRADAY_CALORIES_ENABLED_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_INTRADAY_CALORIES_ENABLED_DISPLAY)

        .define(FITBIT_USER_REPOSITORY_FIRESTORE_FITBIT_COLLECTION_CONFIG,
            Type.STRING,
            FITBIT_USER_REPOSITORY_FIRESTORE_FITBIT_COLLECTION_DEFAULT,
            Importance.LOW,
            FITBIT_USER_REPOSITORY_FIRESTORE_FITBIT_COLLECTION_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USER_REPOSITORY_FIRESTORE_FITBIT_COLLECTION_DISPLAY)

        .define(FITBIT_USER_REPOSITORY_FIRESTORE_USER_COLLECTION_CONFIG,
            Type.STRING,
            FITBIT_USER_REPOSITORY_FIRESTORE_USER_COLLECTION_DEFAULT,
            Importance.LOW,
            FITBIT_USER_REPOSITORY_FIRESTORE_USER_COLLECTION_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_USER_REPOSITORY_FIRESTORE_USER_COLLECTION_DISPLAY)

        .define(FITBIT_MAX_FORBIDDEN_CONFIG,
            Type.INT,
            FITBIT_MAX_FORBIDDEN_DEFAULT,
            Importance.MEDIUM,
            FITBIT_MAX_FORBIDDEN_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_MAX_FORBIDDEN_DISPLAY)
        
        .define(FITBIT_FORBIDDEN_BACKOFF_CONFIG,
            Type.INT,
            FITBIT_FORBIDDEN_BACKOFF_DEFAULT,
            Importance.MEDIUM,
            FITBIT_FORBIDDEN_BACKOFF_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            FITBIT_FORBIDDEN_BACKOFF_DISPLAY)
        ;
  }

  public List<String> getFitbitUsers() {
    return getList(FITBIT_USERS_CONFIG);
  }

  public String getFitbitClient() {
    return getString(FITBIT_API_CLIENT_CONFIG);
  }

  public String getFitbitClientSecret() {
    return getPassword(FITBIT_API_SECRET_CONFIG).value();
  }

  public UserRepository getUserRepository(UserRepository reuse) {
    if (reuse != null && reuse.getClass().equals(getClass(FITBIT_USER_REPOSITORY_CONFIG))) {
      userRepository = reuse;
    } else {
      userRepository = createUserRepository();
    }
    userRepository.initialize(this);
    return userRepository;
  }

  public UserRepository getUserRepository() {
    userRepository.initialize(this);
    return userRepository;
  }

  @SuppressWarnings("unchecked")
  public UserRepository createUserRepository() {
    try {
      return ((Class<? extends UserRepository>)
          getClass(FITBIT_USER_REPOSITORY_CONFIG)).getDeclaredConstructor().newInstance();
    } catch (IllegalAccessException | InstantiationException
        | InvocationTargetException | NoSuchMethodException e) {
      throw new ConnectException("Invalid class for: " + SOURCE_PAYLOAD_CONVERTER_CONFIG, e);
    }
  }

  public String getFitbitIntradayStepsTopic() {
    return getString(FITBIT_INTRADAY_STEPS_TOPIC_CONFIG);
  }

  public Boolean getFitbitIntradayStepsEnabled() {
    return getBoolean(FITBIT_INTRADAY_STEPS_ENABLED_CONFIG);
  }

  public String getFitbitSleepStagesTopic() {
    return getString(FITBIT_SLEEP_STAGES_TOPIC_CONFIG);
  }

  public Boolean getFitbitSleepStagesEnabled() {
    return getBoolean(FITBIT_SLEEP_STAGES_ENABLED_CONFIG);
  }

  public String getFitbitTimeZoneTopic() {
    return getString(FITBIT_TIME_ZONE_TOPIC_CONFIG);
  }

  public Boolean getFitbitTimeZoneEnabled() {
    return getBoolean(FITBIT_TIME_ZONE_ENABLED_CONFIG);
  }

  public Boolean getFitbitActivityLogEnabled() {
    return getBoolean(FITBIT_ACTIVITY_LOG_ENABLED_CONFIG);
  }

  public String getFitbitIntradayHeartRateTopic() {
    return getString(FITBIT_INTRADAY_HEART_RATE_TOPIC_CONFIG);
  }

  public Boolean getFitbitIntradayHeartRateEnabled() {
    return getBoolean(FITBIT_INTRADAY_HEART_RATE_ENABLED_CONFIG);
  }

  public String getFitbitIntradayHeartRateVariabilityTopic() {
    return getString(FITBIT_INTRADAY_HEART_RATE_VARIABILITY_TOPIC_CONFIG);
  }

  public Boolean getFitbitIntradayHeartRateVariabilityEnabled() {
    return getBoolean(FITBIT_INTRADAY_HEART_RATE_VARIABILITY_ENABLED_CONFIG);
  }

  public String getFitbitIntradaySpo2Topic() {
    return getString(FITBIT_INTRADAY_SPO2_TOPIC_CONFIG);
  }

  public Boolean getFitbitIntradaySpo2Enabled() {
    return getBoolean(FITBIT_INTRADAY_SPO2_ENABLED_CONFIG);
  }

  public String getFitbitBreathingRateTopic() {
    return getString(FITBIT_BREATHING_RATE_TOPIC_CONFIG);
  }

  public Boolean getFitbitBreathingRateEnabled() {
    return getBoolean(FITBIT_BREATHING_RATE_ENABLED_CONFIG);
  }

  public String getFitbitSkinTemperatureTopic() {
    return getString(FITBIT_SKIN_TEMPERATURE_TOPIC_CONFIG);
  }

  public Boolean getFitbitSkinTemperatureEnabled() {
    return getBoolean(FITBIT_SKIN_TEMPERATURE_ENABLED_CONFIG);
  }

  public String getFitbitRestingHeartRateTopic() {
    return getString(FITBIT_RESTING_HEART_RATE_TOPIC_CONFIG);
  }

  public Boolean getFitbitRestingHeartRateEnabled() {
    return getBoolean(FITBIT_RESTING_HEART_RATE_ENABLED_CONFIG);
  }

  public String getFitbitSleepClassicTopic() {
    return getString(FITBIT_SLEEP_CLASSIC_TOPIC_CONFIG);
  }

  public Boolean getFitbitSleepClassicEnabled() {
    return getBoolean(FITBIT_SLEEP_CLASSIC_ENABLED_CONFIG);
  }

  public Path getFitbitUserCredentialsPath() {
    return Paths.get(getString(FITBIT_USER_CREDENTIALS_DIR_CONFIG));
  }

  public HttpUrl getFitbitUserRepositoryUrl() {
    String urlString = getString(FITBIT_USER_REPOSITORY_URL_CONFIG).trim();
    if (urlString.charAt(urlString.length() - 1) != '/') {
      urlString += '/';
    }
    HttpUrl url = HttpUrl.parse(urlString);
    if (url == null) {
      throw new ConfigException(FITBIT_USER_REPOSITORY_URL_CONFIG,
          getString(FITBIT_USER_REPOSITORY_URL_CONFIG),
          "User repository URL " + urlString + " cannot be parsed as URL.");
    }
    return url;
  }

  public Headers getClientCredentials() {
    return clientCredentials;
  }

  public String getActivityLogTopic() {
    return getString(FITBIT_ACTIVITY_LOG_TOPIC_CONFIG);
  }

  public boolean hasIntradayAccess() {
    return getBoolean(FITBIT_API_INTRADAY_ACCESS_CONFIG);
  }

  public Duration getPollIntervalPerUser() {
    return Duration.ofSeconds(getInt(FITBIT_USER_POLL_INTERVAL));
  }

  public Duration getTooManyRequestsCooldownInterval() {
    return Duration.ofHours(1);
  }

  public String getFitbitIntradayCaloriesTopic() {
    return getString(FITBIT_INTRADAY_CALORIES_TOPIC_CONFIG);
  }

  public Boolean getFitbitIntradayCaloriesEnabled() {
    return getBoolean(FITBIT_INTRADAY_CALORIES_ENABLED_CONFIG);
  }

  public String getFitbitUserRepositoryFirestoreFitbitCollection() {
    return getString(FITBIT_USER_REPOSITORY_FIRESTORE_FITBIT_COLLECTION_CONFIG);
  }

  public String getFitbitUserRepositoryFirestoreUserCollection() {
    return getString(FITBIT_USER_REPOSITORY_FIRESTORE_USER_COLLECTION_CONFIG);
  }

  public String getFitbitUserRepositoryClientId() {
    return getString(FITBIT_USER_REPOSITORY_CLIENT_ID_CONFIG);
  }

  public String getFitbitUserRepositoryClientSecret() {
    return getPassword(FITBIT_USER_REPOSITORY_CLIENT_SECRET_CONFIG).value();
  }

  public URL getFitbitUserRepositoryTokenUrl() {
    String value = getString(FITBIT_USER_REPOSITORY_TOKEN_URL_CONFIG);
    if (value == null || value.isEmpty()) {
      return null;
    } else {
      try {
        return new URL(getString(FITBIT_USER_REPOSITORY_TOKEN_URL_CONFIG));
      } catch (MalformedURLException ex) {
        throw new ConfigException("Fitbit user repository token URL is invalid.");
      }
    }
  }

  public int getMaxForbidden() {
    return getInt(FITBIT_MAX_FORBIDDEN_CONFIG);
  }

  public int getForbiddenBackoff() {
    return getInt(FITBIT_FORBIDDEN_BACKOFF_CONFIG);
  }
}
