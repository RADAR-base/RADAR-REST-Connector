package org.radarbase.connect.rest.fitbit.user.firebase;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.NotAuthorizedException;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitTokenService {

  private static final Logger logger = LoggerFactory.getLogger(FitbitTokenService.class);
  private final OkHttpClient client;
  private final ObjectMapper mapper;
  private final String clientId;
  private final String clientSecret;
  private final String tokenEndpoint;

  public FitbitTokenService(String clientId, String clientSecret, String tokenEndpoint) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tokenEndpoint = tokenEndpoint;
    this.mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    this.client =
        new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
  }

  public FitbitOAuth2UserCredentials processTokenRequest(FormBody form)
      throws NotAuthorizedException, IOException {
    String credentials = Credentials.basic(clientId, clientSecret);

    Request request =
        new Request.Builder()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", credentials)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .url(tokenEndpoint)
            .post(form)
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
          logger.error("Got empty response body");
          throw new IOException("No response from server");
        }

        try {
          String responseBodyStr = responseBody.string();
          logger.debug("Response: {}", responseBodyStr);
          return mapper.readValue(responseBodyStr, FitbitOAuth2UserCredentials.class);
        } catch (IOException e) {
          logger.error("Error when deserializing response.", e);
          throw new NotAuthorizedException("Cannot read token response", e);
        }
      } else {
        throw new NotAuthorizedException(
            "Failed to execute the request : Response-code :"
                + response.code()
                + " received when requesting token from server with "
                + "message "
                + response.message());
      }
    }
  }

  public FitbitOAuth2UserCredentials refreshToken(String oldToken) throws IOException {
    FormBody form =
        new FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", oldToken)
            .build();
    return this.processTokenRequest(form);
  }
}
