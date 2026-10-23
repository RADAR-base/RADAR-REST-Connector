package org.radarbase.googlehealth.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class RestOauth2AccessToken(
    @param:JsonProperty("accessToken") val accessToken: String,
    @param:JsonProperty("expiresAt") val expiresAt: Instant? = null,
)
