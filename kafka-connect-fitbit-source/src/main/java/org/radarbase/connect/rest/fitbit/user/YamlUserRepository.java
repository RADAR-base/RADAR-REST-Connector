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

package org.radarbase.connect.rest.fitbit.user;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.MIN_INSTANT;
import static org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator.JSON_READER;
import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrRethrow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User repository that reads and writes configuration of YAML files in a local directory. The
 * directory will be recursively scanned for all YAML files. Those should all contain
 * {@link LocalUser} serializations.
 */
public class YamlUserRepository implements UserRepository {
  private static final Logger logger = LoggerFactory.getLogger(YamlUserRepository.class);
  private static final YAMLFactory YAML_FACTORY = new YAMLFactory();
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAML_FACTORY);
  static {
    YAML_MAPPER.registerModule(new JavaTimeModule());
    YAML_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }
  private static final ObjectReader USER_READER = YAML_MAPPER.readerFor(LocalUser.class);
  private static final ObjectWriter USER_WRITER = YAML_MAPPER.writerFor(LocalUser.class);
  private static final Duration FETCH_THRESHOLD = Duration.ofHours(1L);

  private static final int NUM_RETRIES = 10;


  private final OkHttpClient client;

  private Set<String> configuredUsers;
  private Headers headers;
  private ConcurrentMap<String, LockedUser> users = new ConcurrentHashMap<>();
  private final AtomicReference<Instant> nextFetch = new AtomicReference<>(MIN_INSTANT);
  private Path credentialsDir;

  public YamlUserRepository() {
    this.client = new OkHttpClient();
  }

  @Override
  public User get(String key) {
    updateUsers();
    LockedUser user = users.get(key);
    if (user == null) {
      return null;
    } else {
      return user.apply(LocalUser::copy);
    }
  }

  private void updateUsers() {
    Instant nextFetchTime = nextFetch.get();
    Instant now = Instant.now();
    if (!now.isAfter(nextFetchTime)
        || !nextFetch.compareAndSet(nextFetchTime, now.plus(FETCH_THRESHOLD))) {
      return;
    }
    forceUpdateUsers();
  }

  private void forceUpdateUsers() {
    try {
      Map<String, LockedUser> newMap = Files.walk(credentialsDir)
          .filter(p -> Files.isRegularFile(p)
              && p.getFileName().toString().toLowerCase().endsWith(".yml"))
          .map(tryOrRethrow(p -> new LockedUser(USER_READER.readValue(p.toFile()), p)))
          .collect(Collectors.toMap(l -> l.user.getId(), Function.identity()));

      users.keySet().removeIf(u -> !newMap.containsKey(u));
      newMap.forEach(users::putIfAbsent);
    } catch (IOException | RuntimeException ex) {
      logger.error("Failed to read user directories: {}", ex.toString());
    }
  }

  @Override
  public Stream<LocalUser> stream() {
    Stream<LockedUser> users = this.users.values().stream()
        .filter(lockedTest(u -> u.getOAuth2Credentials().hasRefreshToken()));
    if (!configuredUsers.isEmpty()) {
      users = users.filter(lockedTest(u -> configuredUsers.contains(u.getVersionedId())));
    }
    return users.map(lockedApply(LocalUser::copy));
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    this.credentialsDir = ((FitbitRestSourceConnectorConfig) config)
        .getFitbitUserCredentialsPath();
    try {
      this.credentialsDir = ((FitbitRestSourceConnectorConfig)config).getFitbitUserCredentialsPath();
      Files.createDirectories(this.credentialsDir);
    } catch (IOException ex) {
      throw new ConfigException("Failed to read user directory " + credentialsDir, ex);
    }
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    configuredUsers = new HashSet<>(fitbitConfig.getFitbitUsers());
    headers = ((FitbitRestSourceConnectorConfig) config).getClientCredentials();
  }

  @Override
  public String getAccessToken(User user) throws IOException, NotAuthorizedException {
    updateUsers();
    LockedUser actualUser = this.users.get(user.getId());
    if (actualUser == null) {
      throw new NoSuchElementException("User " + user + " is not present in this user repository.");
    }
    String currentToken = actualUser.apply(u -> {
      if (!u.getOAuth2Credentials().isAccessTokenExpired()) {
        return u.getOAuth2Credentials().getAccessToken();
      } else {
        return null;
      }
    });

    return currentToken != null ? currentToken : refreshAccessToken(user);
  }

  @Override
  public String refreshAccessToken(User user) throws IOException {
    return refreshAccessToken(user, NUM_RETRIES);
  }

  @Override
  public boolean hasPendingUpdates() {
    Instant nextFetchTime = nextFetch.get();
    Instant now = Instant.now();
    return now.isAfter(nextFetchTime);
  }

  @Override
  public void applyPendingUpdates() {
    forceUpdateUsers();
    nextFetch.set(Instant.now().plus(FETCH_THRESHOLD));
  }

  /**
   * Refreshes the Fitbit access token on the current host, using the locally stored refresh token.
   * If successful, the tokens are locally stored.
   * If the refresh token is expired or invalid, the access token and the refresh token are set to
   * null.
   * @param user user to request access token for.
   * @param retry number of retries before exiting.
   * @return access token
   * @throws IOException if the refresh fails
   * @throws NotAuthorizedException if no refresh token is stored with the user or if the
   *                                current refresh token is no longer valid.
   */
  public synchronized String refreshAccessToken(User user, int retry) throws IOException {
    LockedUser actualUser = this.users.get(user.getId());
    if (actualUser == null) {
      throw new NoSuchElementException("User " + user + " is not present in this user repository.");
    }
    String refreshToken = actualUser.apply(u -> u.getOAuth2Credentials().getRefreshToken());
    if (refreshToken == null || refreshToken.isEmpty()) {
      throw new NotAuthorizedException("Refresh token is not set");
    }
    Request request = new Request.Builder()
        .url("https://api.fitbit.com/oauth2/token")
        .headers(headers)
        .post(new FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build())
        .build();

    try (Response response = client.newCall(request).execute()) {
      ResponseBody responseBody = response.body();

      if (response.isSuccessful() && responseBody != null) {
        JsonNode node;
        try {
          node = JSON_READER.readTree(responseBody.charStream());
        } catch (IOException ex) {
          if (retry > 0) {
            logger.warn("Failed to read OAuth 2.0 response: {}", ex.toString());
            return refreshAccessToken(user, retry - 1);
          }
          throw ex;
        }

        JsonNode expiresInNode = node.get("expires_in");
        Long expiresIn = expiresInNode != null
            ? expiresInNode.asLong()
            : null;

        JsonNode accessTokenNode = node.get("access_token");
        JsonNode refreshTokenNode = node.get("refresh_token");
        if (accessTokenNode == null || refreshTokenNode == null) {
          if (retry > 0) {
            logger.warn("Failed to get access token in successful OAuth 2.0 request:"
                + " access token or refresh token are missing");
            return refreshAccessToken(user, retry - 1);
          } else {
            throw new NotAuthorizedException("Did not get an access token");
          }
        }

        actualUser.accept((u, p) -> {
          if (!refreshToken.equals(u.getOAuth2Credentials().getRefreshToken())) {
            // it was updated already by another thread.
            return;
          }
          u.setOauth2Credentials(new OAuth2UserCredentials(
              refreshTokenNode.asText(), accessTokenNode.asText(), expiresIn));
          store(p, u);
        });
      } else if (response.code() == 400 || response.code() == 401) {
        actualUser.accept((u, p) -> {
          if (!refreshToken.equals(u.getOAuth2Credentials().getRefreshToken())) {
            // it was updated already by another thread.
            return;
          }
          u.setOauth2Credentials(new OAuth2UserCredentials());
          store(p, u);
        });
        throw new NotAuthorizedException("Refresh token is no longer valid.");
      } else {
        String message = "Failed to request refresh token, with response HTTP status code "
            + response.code();
        if (responseBody != null) {
          message += " and content " + responseBody.string();
        }
        throw new IOException(message);
      }
    }
    return actualUser.apply(u -> u.getOAuth2Credentials().getAccessToken());
  }

  /**
   * Store a user to given path.
   * @param path path to store at.
   * @param user use to store.
   */
  private void store(Path path, LocalUser user) {
    try {
      Path temp = Files.createTempFile(user.getId(), ".tmp");
      try {
        try (OutputStream out = Files.newOutputStream(temp)) {
          synchronized (this) {
            USER_WRITER.writeValue(out, user);
          }
        }
        Files.move(temp, path, REPLACE_EXISTING);
      } finally {
        Files.deleteIfExists(temp);
      }
    } catch (IOException ex) {
      logger.error("Failed to store user file: {}", ex.toString());
    }
  }

  /**
   * Local user that is protected by a multi-threading lock to avoid simultaneous IO
   * and modifications.
   */
  private static final class LockedUser {
    final Lock lock = new ReentrantLock();
    final LocalUser user;
    final Path path;

    LockedUser(LocalUser user, Path path) {
      this.user = user;
      this.path = path;
    }

    <V> V apply(Function<? super LocalUser, ? extends V> func) {
      lock.lock();
      try {
        return func.apply(user);
      } finally {
        lock.unlock();
      }
    }

    void accept(BiConsumer<? super LocalUser, ? super Path> consumer) {
      lock.lock();
      try {
        consumer.accept(user, path);
      } finally {
        lock.unlock();
      }
    }
  }

  private static <V> Function<LockedUser, V> lockedApply(Function<? super LocalUser, ? extends V> func) {
    return l -> l.apply(func);
  }

  private static Predicate<LockedUser> lockedTest(Predicate<? super LocalUser> func) {
    return l -> {
      l.lock.lock();
      try {
        return func.test(l.user);
      } finally {
        l.lock.unlock();
      }
    };
  }
}
