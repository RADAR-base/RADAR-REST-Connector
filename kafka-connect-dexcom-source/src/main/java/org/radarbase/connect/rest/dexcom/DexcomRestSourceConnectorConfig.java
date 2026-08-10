package org.radarbase.connect.rest.dexcom;

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
import org.radarbase.connect.rest.dexcom.user.DexcomServiceUserRepository;

public class DexcomRestSourceConnectorConfig extends AbstractConfig {

  private static final String DEXCOM_EGV_ENABLED_CONFIG = "dexcom.egv.enabled";
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

  public DexcomRestSourceConnectorConfig(Map<?, ?> originals) {
    super(conf(), originals);
  }

  public static ConfigDef conf() {
    return new ConfigDef()
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
        .define(DEXCOM_EGV_ENABLED_CONFIG, Type.BOOLEAN, true, Importance.LOW, "...");
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
}
