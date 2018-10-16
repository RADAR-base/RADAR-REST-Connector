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

import static org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator.JSON_READER;

import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import javax.ws.rs.NotAuthorizedException;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;

@SuppressWarnings("unused")
public class ServiceUserRepository implements FitbitUserRepository {
  private static final ObjectReader USER_LIST_READER = JSON_READER.forType(FitbitUsers.class);
  private static final ObjectReader USER_READER = JSON_READER.forType(FitbitUser.class);
  private static final ObjectReader OAUTH_READER = JSON_READER.forType(OAuth2UserCredentials.class);
  private static final RequestBody EMPTY_BODY = RequestBody.create(
      MediaType.parse("application/json; charset=utf-8"), "");
  private final OkHttpClient client;
  private HttpUrl baseUrl;
  private final Map<String, OAuth2UserCredentials> cachedCredentials;
  private HashSet<String> containedUsers;

  public ServiceUserRepository() {
    this.client = new OkHttpClient();
    this.cachedCredentials = new HashMap<>();
    this.containedUsers = new HashSet<>();
  }

  @Override
  public FitbitUser get(String key) throws IOException {
    Request request = requestFor("registration/users/" + key).build();
    return makeRequest(request, USER_READER);
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    this.baseUrl = fitbitConfig.getFitbitUserRepositoryUrl();
    this.containedUsers.addAll(fitbitConfig.getFitbitUsers());
  }

  @Override
  public Stream<? extends FitbitUser> stream() throws IOException {
    Request request = requestFor("registration/users").build();
    return this.<FitbitUsers>makeRequest(request, USER_LIST_READER).getUsers().stream()
        .filter(u -> containedUsers.contains(u.getId()));
  }

  @Override
  public String getAccessToken(FitbitUser user) throws IOException, NotAuthorizedException {
    OAuth2UserCredentials credentials = cachedCredentials.get(user.getId());
    if (credentials == null || credentials.isAccessTokenExpired()) {
      Request request = requestFor("registration/users/" + user + "/token").build();
      credentials = makeRequest(request, OAUTH_READER);
      cachedCredentials.put(user.getId(), credentials);
    }
    return credentials.getAccessToken();
  }

  @Override
  public String refreshAccessToken(FitbitUser user) throws IOException, NotAuthorizedException {
    Request request = requestFor("registration/users/" + user + "/token")
      .post(EMPTY_BODY)
      .build();
    OAuth2UserCredentials credentials = makeRequest(request, OAUTH_READER);
    cachedCredentials.put(user.getId(), credentials);
    return credentials.getAccessToken();
  }

  private Request.Builder requestFor(String relativeUrl) {
    HttpUrl url = baseUrl.resolve(relativeUrl);
    if (url == null) {
      throw new IllegalArgumentException("Relative URL is invalid");
    }
    return new Request.Builder().url(url);
  }

  private <T> T makeRequest(Request request, ObjectReader reader) throws IOException {
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
      return reader.readValue(body.charStream());
    }
  }
}
