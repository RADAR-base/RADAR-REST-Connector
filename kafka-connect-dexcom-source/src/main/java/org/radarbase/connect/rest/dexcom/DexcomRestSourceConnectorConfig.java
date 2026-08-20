package org.radarbase.connect.rest.dexcom;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import okhttp3.HttpUrl;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.radarbase.connect.rest.dexcom.user.DexcomServiceUserRepository;
import org.radarbase.connect.rest.dexcom.user.DexcomUserRepository;
import org.radarbase.dexcom.route.DexcomRoute;

public class DexcomRestSourceConnectorConfig extends AbstractConfig {

  private static final String DEXCOM_EGV_ENABLED_CONFIG = "dexcom.egv.enabled";
  private static final String DEXCOM_CALIBRATION_ENABLED_CONFIG = "dexcom.calibration.enabled";
  private static final String DEXCOM_EVENT_ENABLED_CONFIG = "dexcom.event.enabled";
  static final String SOURCE_URL_CONFIG = "rest.source.base.url";
  public static final String DEXCOM_USERS_CONFIG = "dexcom.users";
  public static final String DEXCOM_API_CLIENT_CONFIG = "dexcom.api.client";
  public static final String DEXCOM_API_SECRET_CONFIG = "dexcom.api.secret";
  public static final String DEXCOM_USER_REPOSITORY_CONFIG = "dexcom.user.repository.class";
  public static final String DEXCOM_USER_REPOSITORY_URL_CONFIG = "dexcom.user.repository.url";
  public static final String DEXCOM_USER_REPOSITORY_CLIENT_ID_CONFIG =
      "dexcom.user.repository.client.id";
  public static final String DEXCOM_USER_REPOSITORY_CLIENT_SECRET_CONFIG =
      "dexcom.user.repository.client.secret";
  public static final String DEXCOM_USER_REPOSITORY_TOKEN_URL_CONFIG =
      "dexcom.user.repository.oauth2.token.url";

  private static final String USERS_SELF_PATH = "/v3/users/self";

  private DexcomUserRepository userRepository;

  public DexcomRestSourceConnectorConfig(ConfigDef config, Map<?, ?> originals, boolean doLog) {
    super(config, originals, doLog);
  }

  public DexcomRestSourceConnectorConfig(Map<?, ?> originals, boolean doLog) {
    this(conf(), originals, doLog);
  }

  public DexcomRestSourceConnectorConfig(Map<?, ?> originals) {
    this(originals, true);
  }

  public static ConfigDef conf() {
    return new ConfigDef()
        .define(
            SOURCE_URL_CONFIG,
            Type.STRING,
            DexcomRoute.DEFAULT_API_BASE_URL,
            Importance.HIGH,
            "Dexcom API base URL (host or full .../v3/users/self path).")
        .define(DEXCOM_USERS_CONFIG, Type.LIST, Collections.emptyList(), Importance.HIGH, "...")
        .define(
            DEXCOM_USER_REPOSITORY_CONFIG,
            Type.CLASS,
            DexcomServiceUserRepository.class,
            Importance.MEDIUM,
            "...")
        .define(DEXCOM_USER_REPOSITORY_URL_CONFIG, Type.STRING, "", Importance.LOW, "...")
        .define(DEXCOM_USER_REPOSITORY_CLIENT_ID_CONFIG, Type.STRING, "", Importance.MEDIUM, "...")
        .define(
            DEXCOM_USER_REPOSITORY_CLIENT_SECRET_CONFIG,
            Type.PASSWORD,
            "",
            Importance.MEDIUM,
            "...")
        .define(DEXCOM_USER_REPOSITORY_TOKEN_URL_CONFIG, Type.STRING, "", Importance.MEDIUM, "...")
        .define(DEXCOM_EGV_ENABLED_CONFIG, Type.BOOLEAN, true, Importance.LOW, "...")
        .define(DEXCOM_CALIBRATION_ENABLED_CONFIG, Type.BOOLEAN, true, Importance.LOW, "...")
        .define(DEXCOM_EVENT_ENABLED_CONFIG, Type.BOOLEAN, true, Importance.LOW, "...");
  }

  public List<String> getDexcomUsers() {
    return getList(DEXCOM_USERS_CONFIG);
  }

  public HttpUrl getDexcomUserRepositoryUrl() {
    String urlString = getString(DEXCOM_USER_REPOSITORY_URL_CONFIG).trim();
    if (urlString.isEmpty()) {
      throw new ConfigException(
          DEXCOM_USER_REPOSITORY_URL_CONFIG, urlString, "User repository URL is required.");
    }
    if (urlString.charAt(urlString.length() - 1) != '/') {
      urlString += '/';
    }
    HttpUrl url = HttpUrl.parse(urlString);
    if (url == null) {
      throw new ConfigException(
          DEXCOM_USER_REPOSITORY_URL_CONFIG,
          getString(DEXCOM_USER_REPOSITORY_URL_CONFIG),
          "User repository URL " + urlString + " cannot be parsed as URL.");
    }
    return url;
  }

  public String getDexcomUserRepositoryClientId() {
    return getString(DEXCOM_USER_REPOSITORY_CLIENT_ID_CONFIG);
  }

  public String getDexcomUserRepositoryClientSecret() {
    return getPassword(DEXCOM_USER_REPOSITORY_CLIENT_SECRET_CONFIG).value();
  }

  public URL getDexcomUserRepositoryTokenUrl() {
    String value = getString(DEXCOM_USER_REPOSITORY_TOKEN_URL_CONFIG);
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      return new URL(value);
    } catch (MalformedURLException e) {
      throw new ConfigException("Dexcom user repository token URL is invalid.");
    }
  }

  public boolean getDexcomEgvEnabled() {
    return getBoolean(DEXCOM_EGV_ENABLED_CONFIG);
  }

  public boolean getDexcomCalibrationEnabled() {
    return getBoolean(DEXCOM_CALIBRATION_ENABLED_CONFIG);
  }

  public boolean getDexcomEventEnabled() {
    return getBoolean(DEXCOM_EVENT_ENABLED_CONFIG);
  }

  /**
   * Base URL passed into Dexcom routes. Accepts either a host ({@code
   * https://sandbox-api.dexcom.com}) or a full path ending in {@code /v3/users/self}.
   */
  public String getDexcomApiBaseUrl() {
    String url = getString(SOURCE_URL_CONFIG).trim();
    while (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    if (!url.endsWith(USERS_SELF_PATH)) {
      url = url + USERS_SELF_PATH;
    }
    return url;
  }

  public DexcomUserRepository getUserRepository(DexcomUserRepository reuse) {
    if (reuse != null && reuse.getClass().equals(getClass(DEXCOM_USER_REPOSITORY_CONFIG))) {
      userRepository = reuse;
    } else {
      userRepository = createUserRepository();
    }
    userRepository.initialize(this);
    return userRepository;
  }

  public DexcomUserRepository getUserRepository() {
    if (userRepository == null) {
      userRepository = createUserRepository();
    }
    userRepository.initialize(this);
    return userRepository;
  }

  @SuppressWarnings("unchecked")
  public DexcomUserRepository createUserRepository() {
    try {
      return ((Class<? extends DexcomUserRepository>) getClass(DEXCOM_USER_REPOSITORY_CONFIG))
          .getDeclaredConstructor()
          .newInstance();
    } catch (IllegalAccessException
        | InstantiationException
        | InvocationTargetException
        | NoSuchMethodException e) {
      throw new ConnectException("Invalid user repository class. " + e);
    }
  }
}
