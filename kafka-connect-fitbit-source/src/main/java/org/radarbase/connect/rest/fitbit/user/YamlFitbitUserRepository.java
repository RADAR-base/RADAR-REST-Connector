package org.radarbase.connect.rest.fitbit.user;

import static org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator.JSON_READER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.ws.rs.NotAuthorizedException;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.kafka.common.config.ConfigException;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.config.FitbitUserConfig;
import org.radarbase.connect.rest.fitbit.config.LocalFitbitUser;
import org.radarbase.connect.rest.fitbit.util.SynchronizedFileAccess;

public class YamlFitbitUserRepository implements FitbitUserRepository {
  private static final YAMLFactory YAML_FACTORY = new YAMLFactory();
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAML_FACTORY);
  static {
    YAML_MAPPER.registerModule(new JavaTimeModule());
  }

  private final OkHttpClient client;

  private Set<String> configuredUsers;
  private SynchronizedFileAccess<FitbitUserConfig> users;
  private Headers headers;

  public YamlFitbitUserRepository() {
    this.client = new OkHttpClient();
  }

  @Override
  public FitbitUser get(String key) {
    return users.get().get(key);
  }

  @Override
  public Stream<LocalFitbitUser> stream() {
    Stream<LocalFitbitUser> users = this.users.get().stream();
    if (!configuredUsers.isEmpty()) {
      users = users.filter(u -> configuredUsers.contains(u.getId()));
    }
    return users;
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    Path path = ((FitbitRestSourceConnectorConfig)config).getFitbitUserRepositoryPath();
    try {
      this.users = SynchronizedFileAccess.ofPath(path, YAML_MAPPER, FitbitUserConfig.class);
    } catch (IOException ex) {
      throw new ConfigException("Failed to read user repository " + path, ex);
    }
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    configuredUsers = new HashSet<>(fitbitConfig.getFitbitUsers());
    headers = ((FitbitRestSourceConnectorConfig) config).getClientCredentials();
  }

  /**
   * Refreshes the Fitbit access token on the current host, using the locally stored refresh token.
   * If successful, the tokens are locally stored.
   * If the refresh token is expired or invalid, the access token and the refresh token are set to
   * null.
   * @param user user to request access token for.
   * @return access token
   * @throws IOException if the refresh fails
   * @throws NotAuthorizedException if no refresh token is stored with the user or if the
   *                                current refresh token is no longer valid.
   */
  @Override
  public synchronized String refreshAccessToken(FitbitUser user) throws IOException {
    LocalFitbitUser actualUser = this.users.get().get(user.getId());
    if (actualUser.getRefreshToken() == null || actualUser.getRefreshToken().isEmpty()) {
      throw new NotAuthorizedException("Refresh token is not set");
    }
    Request request = new Request.Builder()
        .url("https://api.fitbit.com/oauth2/token")
        .headers(headers)
        .post(new FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", actualUser.getRefreshToken())
            .build())
        .build();

    try (Response response = client.newCall(request).execute()) {
      ResponseBody responseBody = response.body();

      if (response.isSuccessful() && responseBody != null) {
        JsonNode node = JSON_READER.readTree(responseBody.charStream());
        actualUser.setAccessToken(node.get("access_token").asText());
        actualUser.setRefreshToken(node.get("refresh_token").asText());
      } else if (response.code() == 400 || response.code() == 401) {
        actualUser.setAccessToken(null);
        actualUser.setRefreshToken(null);
        throw new NotAuthorizedException("Refresh token is no longer valid.");
      } else {
        String message = "Failed to request refresh token, with response HTTP status code "
            + response.code();
        if (responseBody != null) {
          message += " and content " + responseBody.string();
        }
        throw new IOException(message);
      }
      this.users.store();
      return user.getAccessToken();
    }
  }
}
