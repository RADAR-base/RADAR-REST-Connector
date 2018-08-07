package org.radarbase.connect.rest.fitbit.request;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.Authenticator;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator.JSON_READER;

public class TokenAuthenticator implements Authenticator {
  private static final MediaType X_WWW_FORM_URLENCODED = MediaType.parse(
      "application/x-www-form-urlencoded; charset=utf-8");
  private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticator.class);

  private final FitbitUser user;
  private final Headers headers;
  private final OkHttpClient client;
  private final FitbitUserRepository userRepository;

  TokenAuthenticator(FitbitUser user, Headers headers, OkHttpClient client,
      FitbitUserRepository userRepository) {
    this.user = user;
    this.headers = headers;
    this.client = client;
    this.userRepository = userRepository;
  }

  @Override
  public Request authenticate(Route requestRoute, Response response) throws IOException {
    if (response.code() != 401 || !refreshAccessToken()) {
      return null;
    }

    userRepository.update(user);

    return response.request().newBuilder()
        .header("Authorization", "Bearer " + user.getAccessToken())
        .build();
  }

  private synchronized boolean refreshAccessToken() {
    Request request = new Request.Builder()
        .url("https://api.fitbit.com/oauth2/token")
        .headers(headers)
        .post(RequestBody.create(X_WWW_FORM_URLENCODED,
            "grant_type=refresh_token&refresh_token=" + user.getRefreshToken()))
        .build();

    try (Response response = client.newCall(request).execute()) {
      ResponseBody responseBody = response.body();
      if (response.isSuccessful() && responseBody != null) {
        JsonNode node = JSON_READER.readTree(responseBody.charStream());
        user.setAccessToken(node.get("access_token").asText());
        user.setRefreshToken(node.get("refresh_token").asText());
      }
      return true;
    } catch (IOException e) {
      logger.warn("Failed to refresh token of Fitbit user {}/user ID {}",
          user.getFitbitUserId(), user.getUserId());
      return false;
    }
  }
}
