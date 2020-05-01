package org.radarbase.connect.rest.fitbit.user.firebase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.firestore.annotation.Exclude;
import com.google.cloud.firestore.annotation.IgnoreExtraProperties;
import com.google.cloud.firestore.annotation.PropertyName;
import java.time.Duration;
import java.time.Instant;

/**
 * Fitbit Credentials POJO to help serialize and deserialize fitbit token data obtained using
 * Firebase SDK. Mostly similar to {@link
 * org.radarbase.connect.rest.fitbit.user.OAuth2UserCredentials} but adds all the properties
 * returned by fitbit when refreshing tokens. Also, add public setter/getter for all properties so
 * that can be used without jackson because Firebase SDK uses its own custom mapper based on {@link
 * PropertyName} annotation. Note this is still compatible with Jackson library.
 *
 * <p>see {@link com.google.cloud.firestore.CustomClassMapper.BeanMapper} for more details.
 */
@IgnoreExtraProperties
@JsonIgnoreProperties(ignoreUnknown = true)
public class FitbitOAuth2UserCredentials {
  protected static final Duration DEFAULT_EXPIRY = Duration.ofHours(1);
  protected static final Duration EXPIRY_TIME_MARGIN = Duration.ofMinutes(5);
  protected Instant expiresAt;

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty private String scope;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("user_id")
  private String userId;

  public FitbitOAuth2UserCredentials() {}

  @Exclude
  @JsonIgnore
  public static Instant getExpiresAt(Duration expiresIn) {
    return Instant.now().plus(expiresIn).minus(EXPIRY_TIME_MARGIN);
  }

  @PropertyName("access_token")
  public String getAccessToken() {
    return accessToken;
  }

  @PropertyName("access_token")
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
    if (expiresAt == null) {
      expiresAt = getExpiresAt(DEFAULT_EXPIRY);
    }
  }

  @PropertyName("refresh_token")
  public String getRefreshToken() {
    return refreshToken;
  }

  @PropertyName("refresh_token")
  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  @PropertyName("expires_in")
  public Long getExpiresIn() {
    return expiresIn;
  }

  @PropertyName("expires_in")
  public void setExpiresIn(Long expiresIn) {
    this.expiresIn = expiresIn;
    this.expiresAt =
        getExpiresAt(
            expiresIn != null && expiresIn > 0L ? Duration.ofSeconds(expiresIn) : DEFAULT_EXPIRY);
  }

  @PropertyName("scope")
  public String getScope() {
    return scope;
  }

  @PropertyName("scope")
  public void setScope(String scope) {
    this.scope = scope;
  }

  @PropertyName("token_type")
  public String getTokenType() {
    return tokenType;
  }

  @PropertyName("token_type")
  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  @PropertyName("user_id")
  public String getUserId() {
    return userId;
  }

  @PropertyName("user_id")
  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Exclude
  @JsonIgnore
  public Boolean isAccessTokenExpired() {
    return expiresAt == null || Instant.now().isAfter(expiresAt);
  }

  @Exclude
  @JsonIgnore
  public boolean hasRefreshToken() {
    return refreshToken != null && !refreshToken.isEmpty();
  }
}
