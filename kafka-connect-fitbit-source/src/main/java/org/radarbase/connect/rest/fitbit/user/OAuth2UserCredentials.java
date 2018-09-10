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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;

public class OAuth2UserCredentials {
  private static final Duration DEFAULT_EXPIRY = Duration.ofHours(1);
  private static final Duration EXPIRY_TIME_MARGIN = Duration.ofMinutes(5);

  @JsonProperty
  private final String accessToken;
  @JsonProperty
  private final String refreshToken;
  @JsonProperty
  private final Instant expiresAt;

  public OAuth2UserCredentials() {
    this(null, null, (Instant)null);
  }

  @JsonCreator
  public OAuth2UserCredentials(
      @JsonProperty("refreshToken") String refreshToken,
      @JsonProperty("accessToken") String accessToken,
      @JsonProperty("expiresAt") Instant expiresAt) {
    this.refreshToken = refreshToken;
    this.accessToken = accessToken;
    this.expiresAt = expiresAt == null ? getExpiresAt(DEFAULT_EXPIRY) : expiresAt;
  }

  public OAuth2UserCredentials(String refreshToken, String accessToken, Long expiresIn) {
    this(refreshToken, accessToken, getExpiresAt(
        expiresIn != null && expiresIn > 0L ? Duration.ofSeconds(expiresIn) : DEFAULT_EXPIRY));
  }

  public String getAccessToken() {
    return accessToken;
  }

  public boolean hasRefreshToken() {
    return refreshToken != null && !refreshToken.isEmpty();
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  protected static Instant getExpiresAt(Duration expiresIn) {
    return Instant.now()
        .plus(expiresIn)
        .minus(EXPIRY_TIME_MARGIN);
  }

  @JsonIgnore
  public boolean isAccessTokenExpired() {
    return Instant.now().isAfter(expiresAt);
  }
}
