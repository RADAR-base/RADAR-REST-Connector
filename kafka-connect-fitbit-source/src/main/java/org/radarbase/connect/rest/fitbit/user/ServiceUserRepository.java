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

import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.MIN_INSTANT;
import static org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator.JSON_READER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import io.confluent.common.config.ConfigException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.NotAuthorizedException;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.kafka.connect.errors.ConnectException;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarcns.exception.TokenException;
import org.radarcns.oauth.OAuth2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class ServiceUserRepository implements UserRepository {
  private static final Logger logger = LoggerFactory.getLogger(ServiceUserRepository.class);

  private static final ObjectReader USER_LIST_READER = JSON_READER.forType(Users.class);
  private static final ObjectReader USER_READER = JSON_READER.forType(User.class);
  private static final ObjectReader OAUTH_READER = JSON_READER.forType(OAuth2UserCredentials.class);
  private static final RequestBody EMPTY_BODY =
      RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "");
  private static final Duration FETCH_THRESHOLD = Duration.ofMinutes(1L);

  private final OkHttpClient client;
  private final Map<String, OAuth2UserCredentials> cachedCredentials;
  private final AtomicReference<Instant> nextFetch = new AtomicReference<>(MIN_INSTANT);

  private HttpUrl baseUrl;
  private final HashSet<String> containedUsers;
  private Set<? extends User> timedCachedUsers = new HashSet<>();
  private OAuth2Client repositoryClient;
  private String basicCredentials;

  public ServiceUserRepository() {
    this.client = new OkHttpClient();
    this.cachedCredentials = new HashMap<>();
    this.containedUsers = new HashSet<>();
  }

  @Override
  public User get(String key) throws IOException {
    Request request = requestFor("users/" + key).build();
    return makeRequest(request, USER_READER);
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    this.baseUrl = fitbitConfig.getFitbitUserRepositoryUrl();
    this.containedUsers.addAll(fitbitConfig.getFitbitUsers());

    URL tokenUrl = fitbitConfig.getFitbitUserRepositoryTokenUrl();
    String clientId = fitbitConfig.getFitbitUserRepositoryClientId();
    String clientSecret = fitbitConfig.getFitbitUserRepositoryClientSecret();

    if (tokenUrl != null) {
      if (clientId.isEmpty()) {
        throw new ConfigException("Client ID for user repository is not set.");
      }
      this.repositoryClient = new OAuth2Client.Builder()
          .credentials(clientId, clientSecret)
          .endpoint(tokenUrl)
          .scopes("SUBJECT.READ")
          .httpClient(client)
          .build();
    } else if (clientId != null) {
      basicCredentials = Credentials.basic(clientId, clientSecret);
    }
  }

  @Override
  public Stream<? extends User> stream() {
    return this.timedCachedUsers.stream();
  }

  @Override
  public String getAccessToken(User user) throws IOException, NotAuthorizedException {
    OAuth2UserCredentials credentials = cachedCredentials.get(user.getId());
    if (credentials == null || credentials.isAccessTokenExpired()) {
      Request request = requestFor("users/" + user.getId() + "/token").build();
      credentials = makeRequest(request, OAUTH_READER);
      cachedCredentials.put(user.getId(), credentials);
    }
    return credentials.getAccessToken();
  }

  @Override
  public String refreshAccessToken(User user) throws IOException, NotAuthorizedException {
    Request request = requestFor("users/" + user.getId() + "/token").post(EMPTY_BODY).build();
    OAuth2UserCredentials credentials = makeRequest(request, OAUTH_READER);
    cachedCredentials.put(user.getId(), credentials);
    return credentials.getAccessToken();
  }

  @Override
  public boolean hasPendingUpdates() {
    Instant nextFetchTime = nextFetch.get();
    Instant now = Instant.now();
    return now.isAfter(nextFetchTime);
  }

  @Override
  public void applyPendingUpdates() throws IOException {
    logger.info("Requesting user information from webservice");
    Request request = requestFor("users?source-type=FitBit").build();
    this.timedCachedUsers =
        this.<Users>makeRequest(request, USER_LIST_READER).getUsers().stream()
            .filter(u -> u.isComplete()
                && (containedUsers.isEmpty()
                || containedUsers.contains(u.getVersionedId())))
            .collect(Collectors.toSet());
    nextFetch.set(Instant.now().plus(FETCH_THRESHOLD));
  }

  private Request.Builder requestFor(String relativeUrl) throws IOException {
    HttpUrl url = baseUrl.resolve(relativeUrl);
    if (url == null) {
      throw new IllegalArgumentException("Relative URL is invalid");
    }
    Request.Builder builder = new Request.Builder().url(url);
    String authorization = requestAuthorization();
    if (authorization != null) {
      builder.addHeader("Authorization", authorization);
    }

    return builder;
  }

  private String requestAuthorization() throws IOException {
    if (repositoryClient != null) {
      try {
        return "Bearer " + repositoryClient.getValidToken().getAccessToken();
      } catch (TokenException ex) {
        throw new IOException(ex);
      }
    } else if (basicCredentials != null) {
      return basicCredentials;
    } else {
      return null;
    }
  }

  private <T> T makeRequest(Request request, ObjectReader reader) throws IOException {
    logger.info("Requesting info from {}", request.url());
    try (Response response = client.newCall(request).execute()) {
      ResponseBody body = response.body();

      if (response.code() == 404) {
        throw new NoSuchElementException("URL " + request.url() + " does not exist");
      } else if (!response.isSuccessful() || body == null) {
        String message = "Failed to make request";
        if (response.code() > 0) {
          message += " (HTTP status code " + response.code() + ')';
        }
        if (body != null) {
          message += body.string();
        }
        throw new IOException(message);
      }
      String bodyString = body.string();
      try {
        return reader.readValue(bodyString);
      } catch (JsonProcessingException ex) {
        logger.error("Failed to parse JSON: {}\n{}", ex.toString(), bodyString);
        throw ex;
      }
    }
  }
}
