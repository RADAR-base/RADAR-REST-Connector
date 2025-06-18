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

package org.radarbase.connect.rest.oura;

import static org.apache.kafka.common.config.ConfigDef.NO_DEFAULT_VALUE;

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
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.NonEmptyString;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Validator;
import org.apache.kafka.common.config.ConfigDef.Width;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.radarbase.connect.rest.oura.user.OuraUserRepository;
import org.radarbase.connect.rest.oura.user.OuraServiceUserRepository;

public class OuraRestSourceConnectorConfig extends AbstractConfig {
  public static final Pattern COLON_PATTERN = Pattern.compile(":");

  private static final String SOURCE_POLL_INTERVAL_CONFIG = "rest.source.poll.interval.ms";
  private static final String SOURCE_POLL_INTERVAL_DOC = "How often to poll the source URL.";
  private static final String SOURCE_POLL_INTERVAL_DISPLAY = "Polling interval";
  private static final Long SOURCE_POLL_INTERVAL_DEFAULT = 60000L;

  static final String SOURCE_URL_CONFIG = "rest.source.base.url";
  private static final String SOURCE_URL_DOC = "Base URL for REST source connector.";
  private static final String SOURCE_URL_DISPLAY = "Base URL for REST source connector.";

  public static final String OURA_USERS_CONFIG = "oura.users";
  private static final String OURA_USERS_DOC =
      "The user ID of Oura users to include in polling, separated by commas. "
          + "Non existing user names will be ignored. "
          + "If empty, all users in the user directory will be used.";
  private static final String OURA_USERS_DISPLAY = "Oura users";

  public static final String OURA_API_CLIENT_CONFIG = "oura.api.client";
  private static final String OURA_API_CLIENT_DOC =
      "Client ID for the Oura API";
  private static final String OURA_API_CLIENT_DISPLAY = "Oura API client ID";

  public static final String OURA_API_SECRET_CONFIG = "oura.api.secret";
  private static final String OURA_API_SECRET_DOC = "Secret for the Oura API client set in oura.api.client.";
  private static final String OURA_API_SECRET_DISPLAY = "Oura API client secret";

  public static final String OURA_USER_REPOSITORY_CONFIG = "oura.user.repository.class";
  private static final String OURA_USER_REPOSITORY_DOC = "Class for managing users and authentication.";
  private static final String OURA_USER_REPOSITORY_DISPLAY = "User repository class";

  public static final String OURA_USER_POLL_INTERVAL = "oura.user.poll.interval";
  private static final String OURA_USER_POLL_INTERVAL_DOC = "Polling interval per Oura user per request route in seconds.";
  // 150 requests per hour -> 2.5 per minute. There are currently 5 paths, that limits us to 1
  // call per route per 2 minutes.
  private static final int OURA_USER_POLL_INTERVAL_DEFAULT = 150;
  private static final String OURA_USER_POLL_INTERVAL_DISPLAY = "Per-user per-route polling interval.";

  public static final String OURA_USER_REPOSITORY_URL_CONFIG = "oura.user.repository.url";
  private static final String OURA_USER_REPOSITORY_URL_DOC = "URL for webservice containing user credentials. Only used if a webservice-based user repository is configured.";
  private static final String OURA_USER_REPOSITORY_URL_DISPLAY = "User repository URL";
  private static final String OURA_USER_REPOSITORY_URL_DEFAULT = "";

  public static final String OURA_USER_REPOSITORY_CLIENT_ID_CONFIG = "oura.user.repository.client.id";
  private static final String OURA_USER_REPOSITORY_CLIENT_ID_DOC = "Client ID for connecting to the service repository.";
  private static final String OURA_USER_REPOSITORY_CLIENT_ID_DISPLAY = "Client ID for user repository.";

  public static final String OURA_USER_REPOSITORY_CLIENT_SECRET_CONFIG = "oura.user.repository.client.secret";
  private static final String OURA_USER_REPOSITORY_CLIENT_SECRET_DOC = "Client secret for connecting to the service repository.";
  private static final String OURA_USER_REPOSITORY_CLIENT_SECRET_DISPLAY = "Client Secret for user repository.";

  public static final String OURA_USER_REPOSITORY_TOKEN_URL_CONFIG = "oura.user.repository.oauth2.token.url";
  private static final String OURA_USER_REPOSITORY_TOKEN_URL_DOC = "OAuth 2.0 token url for retrieving client credentials.";
  private static final String OURA_USER_REPOSITORY_TOKEN_URL_DISPLAY = "OAuth 2.0 token URL.";

  private OuraUserRepository userRepository;
  private final Headers clientCredentials;

  public OuraRestSourceConnectorConfig(ConfigDef config, Map<String, String> parsedConfig, boolean doLog) {
    super(config, parsedConfig, doLog);

    String credentialString = getOuraClient() + ":" + getOuraClientSecret();
    String credentialsBase64 = Base64.getEncoder().encodeToString(
        credentialString.getBytes(StandardCharsets.UTF_8));
    this.clientCredentials = Headers.of("Authorization", "Basic " + credentialsBase64);
  }

  public OuraRestSourceConnectorConfig(Map<String, String> parsedConfig, boolean doLog) {
    this(OuraRestSourceConnectorConfig.conf(), parsedConfig, doLog);
  }

  public static ConfigDef conf() {
    int orderInGroup = 0;
    String group = "Oura";

    Validator nonControlChar = new ConfigDef.NonEmptyStringWithoutControlChars() {
      @Override
      public String toString() {
        return "non-empty string without control characters";
      }
    };

    return new ConfigDef()
    .define(SOURCE_POLL_INTERVAL_CONFIG,
            Type.LONG,
            SOURCE_POLL_INTERVAL_DEFAULT,
            Importance.LOW,
            SOURCE_POLL_INTERVAL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            SOURCE_POLL_INTERVAL_DISPLAY)

        .define(SOURCE_URL_CONFIG,
            Type.STRING,
            NO_DEFAULT_VALUE,
            Importance.HIGH,
            SOURCE_URL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            SOURCE_URL_DISPLAY)

        .define(OURA_USERS_CONFIG,
            Type.LIST,
            Collections.emptyList(),
            Importance.HIGH,
            OURA_USERS_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            OURA_USERS_DISPLAY)

        .define(OURA_API_CLIENT_CONFIG,
            Type.STRING,
            NO_DEFAULT_VALUE,
            new NonEmptyString(),
            Importance.HIGH,
            OURA_API_CLIENT_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            OURA_API_CLIENT_DISPLAY)

        .define(OURA_API_SECRET_CONFIG,
            Type.PASSWORD,
            NO_DEFAULT_VALUE,
            Importance.HIGH,
            OURA_API_SECRET_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            OURA_API_SECRET_DISPLAY)

        .define(OURA_USER_POLL_INTERVAL,
            Type.INT,
            OURA_USER_POLL_INTERVAL_DEFAULT,
            Importance.MEDIUM,
            OURA_USER_POLL_INTERVAL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            OURA_USER_POLL_INTERVAL_DISPLAY)

        .define(OURA_USER_REPOSITORY_CONFIG,
            Type.CLASS,
            OuraServiceUserRepository.class,
            Importance.MEDIUM,
            OURA_USER_REPOSITORY_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            OURA_USER_REPOSITORY_DISPLAY)

        .define(OURA_USER_REPOSITORY_URL_CONFIG,
            Type.STRING,
            OURA_USER_REPOSITORY_URL_DEFAULT,
            Importance.LOW,
            OURA_USER_REPOSITORY_URL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            OURA_USER_REPOSITORY_URL_DISPLAY)

        .define(OURA_USER_REPOSITORY_CLIENT_ID_CONFIG,
            Type.STRING,
            "",
            Importance.MEDIUM,
            OURA_USER_REPOSITORY_CLIENT_ID_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            OURA_USER_REPOSITORY_CLIENT_ID_DISPLAY)

        .define(OURA_USER_REPOSITORY_CLIENT_SECRET_CONFIG,
            Type.PASSWORD,
            "",
            Importance.MEDIUM,
            OURA_USER_REPOSITORY_CLIENT_SECRET_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            OURA_USER_REPOSITORY_CLIENT_SECRET_DISPLAY)

        .define(OURA_USER_REPOSITORY_TOKEN_URL_CONFIG,
            Type.STRING,
            "",
            Importance.MEDIUM,
            OURA_USER_REPOSITORY_TOKEN_URL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            OURA_USER_REPOSITORY_TOKEN_URL_DISPLAY)
        ;
  }

  public List<String> getOuraUsers() {
    return getList(OURA_USERS_CONFIG);
  }

  public String getOuraClient() {
    return getString(OURA_API_CLIENT_CONFIG);
  }

  public String getOuraClientSecret() {
    return getPassword(OURA_API_SECRET_CONFIG).value();
  }

  public OuraUserRepository getUserRepository(OuraUserRepository reuse) {
    if (reuse != null && reuse.getClass().equals(getClass(OURA_USER_REPOSITORY_CONFIG))) {
      userRepository = reuse;
    } else {
      userRepository = createUserRepository();
    }
    userRepository.initialize(this);
    return userRepository;
  }

  public OuraUserRepository getUserRepository() {
    userRepository.initialize(this);
    return userRepository;
  }

  @SuppressWarnings("unchecked")
  public OuraUserRepository createUserRepository() {
    try {
      return ((Class<? extends OuraUserRepository>)
          getClass(OURA_USER_REPOSITORY_CONFIG)).getDeclaredConstructor().newInstance();
    } catch (IllegalAccessException | InstantiationException
        | InvocationTargetException | NoSuchMethodException e) {
      throw new ConnectException("Invalid class. " + e);
    }
  }

  public HttpUrl getOuraUserRepositoryUrl() {
    String urlString = getString(OURA_USER_REPOSITORY_URL_CONFIG).trim();
    if (urlString.charAt(urlString.length() - 1) != '/') {
      urlString += '/';
    }
    HttpUrl url = HttpUrl.parse(urlString);
    if (url == null) {
      throw new ConfigException(OURA_USER_REPOSITORY_URL_CONFIG,
          getString(OURA_USER_REPOSITORY_URL_CONFIG),
          "User repository URL " + urlString + " cannot be parsed as URL.");
    }
    return url;
  }

  public Headers getClientCredentials() {
    return clientCredentials;
  }

  public Duration getPollIntervalPerUser() {
    return Duration.ofSeconds(getInt(OURA_USER_POLL_INTERVAL));
  }

  public Duration getTooManyRequestsCooldownInterval() {
    return Duration.ofHours(1);
  }

  public String getOuraUserRepositoryClientId() {
    return getString(OURA_USER_REPOSITORY_CLIENT_ID_CONFIG);
  }

  public String getOuraUserRepositoryClientSecret() {
    return getPassword(OURA_USER_REPOSITORY_CLIENT_SECRET_CONFIG).value();
  }

  public URL getOuraUserRepositoryTokenUrl() {
    String value = getString(OURA_USER_REPOSITORY_TOKEN_URL_CONFIG);
    if (value == null || value.isEmpty()) {
      return null;
    } else {
      try {
        return new URL(getString(OURA_USER_REPOSITORY_TOKEN_URL_CONFIG));
      } catch (MalformedURLException e) {
        throw new ConfigException("Oura user repository token URL is invalid.");
      }
    }
  }
}