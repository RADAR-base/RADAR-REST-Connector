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

package org.radarbase.connect.rest.fitbit.request;

import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserNotAuthorizedException;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticator for Fitbit, which tries to refresh the access token if a request is unauthorized.
 */
public class TokenAuthenticator implements Authenticator {
  private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticator.class);

  private final User user;
  private final UserRepository userRepository;

  TokenAuthenticator(User user, UserRepository userRepository) {
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
    } catch (UserNotAuthorizedException ex) {
      logger.error("Cannot get a new refresh token for user {}. Cancelling request.", user, ex);
      return null;
    }
  }
}
