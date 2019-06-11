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

import static org.apache.kafka.common.config.ConfigDef.NO_DEFAULT_VALUE;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.NonEmptyString;
import org.apache.kafka.common.config.ConfigDef.Range;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Validator;
import org.apache.kafka.common.config.ConfigDef.Width;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.config.ValidClass;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.connect.rest.fitbit.user.YamlUserRepository;

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

  private static final String FITBIT_INTRADAY_HEART_RATE_TOPIC_CONFIG = "fitbit.intraday.heart.rate.topic";
  private static final String FITBIT_INTRADAY_HEART_RATE_TOPIC_DOC = "Topic for Fitbit intraday heart_rate";
  private static final String FITBIT_INTRADAY_HEART_RATE_TOPIC_DISPLAY = "Intraday heartrate topic";
  private static final String FITBIT_INTRADAY_HEART_RATE_TOPIC_DEFAULT = "connect_fitbit_intraday_heart_rate";

  private static final String FITBIT_SLEEP_STAGES_TOPIC_CONFIG = "fitbit.sleep.stages.topic";
  private static final String FITBIT_SLEEP_STAGES_TOPIC_DOC = "Topic for Fitbit sleep stages";
  private static final String FITBIT_SLEEP_STAGES_TOPIC_DEFAULT = "connect_fitbit_sleep_stages";
  private static final String FITBIT_SLEEP_STAGES_TOPIC_DISPLAY = "Sleep stages topic";

  private static final String FITBIT_SLEEP_CLASSIC_TOPIC_CONFIG = "fitbit.sleep.classic.topic";
  private static final String FITBIT_SLEEP_CLASSIC_TOPIC_DOC = "Topic for Fitbit sleep classic data";
  private static final String FITBIT_SLEEP_CLASSIC_TOPIC_DEFAULT = "connect_fitbit_sleep_classic";
  private static final String FITBIT_SLEEP_CLASSIC_TOPIC_DISPLAY = "Classic sleep topic";

  private static final String FITBIT_TIME_ZONE_TOPIC_CONFIG = "fitbit.time.zone.topic";
  private static final String FITBIT_TIME_ZONE_TOPIC_DOC = "Topic for Fitbit profile time zone";
  private static final String FITBIT_TIME_ZONE_TOPIC_DEFAULT = "connect_fitbit_time_zone";
  private static final String FITBIT_TIME_ZONE_TOPIC_DISPLAY = "Time zone topic";

  private static final String FITBIT_ACTIVITY_LOG_TOPIC_CONFIG = "fitbit.activity.log.topic";
  private static final String FITBIT_ACTIVITY_LOG_TOPIC_DOC = "Topic for Fitbit activity log.";
  private static final String FITBIT_ACTIVITY_LOG_TOPIC_DEFAULT = "connect_fitbit_activity_log";
  private static final String FITBIT_ACTIVITY_LOG_TOPIC_DISPLAY = "Activity log topic";

  private final UserRepository userRepository;
  private final Headers clientCredentials;

  @SuppressWarnings("unchecked")
  public FitbitRestSourceConnectorConfig(ConfigDef config, Map<String, String> parsedConfig, boolean doLog) {
    super(config, parsedConfig, doLog);

    try {
      userRepository = ((Class<? extends UserRepository>)
          getClass(FITBIT_USER_REPOSITORY_CONFIG)).getDeclaredConstructor().newInstance();
    } catch (IllegalAccessException | InstantiationException
        | InvocationTargetException | NoSuchMethodException e) {
      throw new ConnectException("Invalid class for: " + SOURCE_PAYLOAD_CONVERTER_CONFIG, e);
    }

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

  public UserRepository getUserRepository() {
    userRepository.initialize(this);
    return userRepository;
  }

  public String getFitbitIntradayStepsTopic() {
    return getString(FITBIT_INTRADAY_STEPS_TOPIC_CONFIG);
  }

  public String getFitbitSleepStagesTopic() {
    return getString(FITBIT_SLEEP_STAGES_TOPIC_CONFIG);
  }

  public String getFitbitTimeZoneTopic() {
    return getString(FITBIT_TIME_ZONE_TOPIC_CONFIG);
  }

  public String getFitbitIntradayHeartRateTopic() {
    return getString(FITBIT_INTRADAY_HEART_RATE_TOPIC_CONFIG);
  }
  public String getFitbitSleepClassicTopic() {
    return getString(FITBIT_SLEEP_CLASSIC_TOPIC_CONFIG);
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
}
