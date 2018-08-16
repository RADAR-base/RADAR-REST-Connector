package org.radarbase.connect.rest.fitbit.user;

import java.io.IOException;
import java.util.stream.Stream;
import javax.ws.rs.NotAuthorizedException;
import org.radarbase.connect.rest.config.RestSourceTool;

/**
 * User repository for Fitbit users.
 */
public interface FitbitUserRepository extends RestSourceTool {
  /**
   * Get specified Fitbit user.
   * @throws IOException if the user cannot be retrieved from the repository.
   */
  FitbitUser get(String key) throws IOException;

  /**
   * Get all relevant Fitbit users.
   * @throws IOException if the list cannot be retrieved from the repository.
   */
  Stream<? extends FitbitUser> stream() throws IOException;

  /**
   * Refresh the access token of given user.
   * @throws IOException if the new access token cannot be retrieved from the repository.
   * @throws NotAuthorizedException if the refresh token is no longer valid. Manual action
   *                                should be taken to get a new refresh token.
   */
  String refreshAccessToken(FitbitUser user) throws IOException, NotAuthorizedException;
}
