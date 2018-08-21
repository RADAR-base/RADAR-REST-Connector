package org.radarbase.connect.rest.fitbit.request;

import java.io.IOException;
import javax.ws.rs.NotAuthorizedException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticator for Fitbit, which tries to refresh the access token if a request is unauthorized.
 */
public class TokenAuthenticator implements Authenticator {
  private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticator.class);

  private final FitbitUser user;
  private final FitbitUserRepository userRepository;

  TokenAuthenticator(FitbitUser user, FitbitUserRepository userRepository) {
    this.user = user;
    this.userRepository = userRepository;
  }

  @Override
  public Request authenticate(Route requestRoute, Response response) throws IOException {
    if (response.code() != 401) {
      return null;
    }

    try {
      String newAccessToken = userRepository.refreshAccessToken(user);

      return response.request().newBuilder()
          .header("Authorization", "Bearer " + newAccessToken)
          .build();
    } catch (NotAuthorizedException ex) {
      logger.error("Cannot get a new refresh token for user {}. Cancelling request.", user);
      return null;
    }
  }
}
