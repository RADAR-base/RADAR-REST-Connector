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

import java.io.IOException;
import java.util.stream.Stream;
import javax.ws.rs.NotAuthorizedException;
import org.radarbase.connect.rest.config.RestSourceTool;

/**
 * User repository for Fitbit users.
 */
public interface UserRepository extends RestSourceTool {
  /**
   * Get specified Fitbit user.
   * @throws IOException if the user cannot be retrieved from the repository.
   */
  User get(String key) throws IOException;

  /**
   * Get all relevant Fitbit users.
   * @throws IOException if the list cannot be retrieved from the repository.
   */
  Stream<? extends User> stream() throws IOException;

  /**
   * Get the current access token of given user. If it has expired, a new access token will
   * be requested. If the server indicates the token is invalid, call
   * {@link #refreshAccessToken(User)} instead.
   *
   * @throws IOException if the new access token cannot be retrieved from the repository.
   * @throws NotAuthorizedException if the refresh token is no longer valid. Manual action
   *                                should be taken to get a new refresh token.
   * @throws java.util.NoSuchElementException if the user does not exists in this repository.
   */
  String getAccessToken(User user) throws IOException, NotAuthorizedException;

  /**
   * Refresh the access token of given user.
   * @throws IOException if the new access token cannot be retrieved from the repository.
   * @throws NotAuthorizedException if the refresh token is no longer valid. Manual action
   *                                should be taken to get a new refresh token.
   * @throws java.util.NoSuchElementException if the user does not exists in this repository.
   */
  String refreshAccessToken(User user) throws IOException, NotAuthorizedException;

  /**
   * The functions allows the repository to supply when there are pending updates.
   * This gives more control to the user repository in updating and caching users.
   * @return {@code true} if there are new updates available, {@code false} otherwise.
   */
  boolean hasPendingUpdates();

  /**
   * Apply any pending updates to users. This could include, for instance, refreshing a cache
   * of users with latest information.
   * This is called when {@link #hasPendingUpdates()} is {@code true}.
   * @throws IOException if there was an error when applying updates.
   */
  void applyPendingUpdates() throws IOException;
}
