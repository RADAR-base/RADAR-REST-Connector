package org.radarbase.oura.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.radarcns.kafka.ObservationKey
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class OuraUser(
    @JsonProperty("id") override val id: String,
    @JsonProperty("createdAt") override val createdAt: Instant,
    @JsonProperty("projectId") override val projectId: String,
    @JsonProperty("userId") override val userId: String,
    @JsonProperty("humanReadableUserId") override val humanReadableUserId: String?,
    @JsonProperty("sourceId") override val sourceId: String,
    @JsonProperty("externalId") override val externalId: String?,
    @JsonProperty("isAuthorized") override val isAuthorized: Boolean,
    @JsonProperty("startDate") override val startDate: Instant,
    @JsonProperty("endDate") override val endDate: Instant? = null,
    @JsonProperty("version") override val version: String? = null,
    @JsonProperty("serviceUserId") override val serviceUserId: String? = null,
) : User {
    override val observationKey: ObservationKey = ObservationKey(projectId, userId, sourceId)
    override val versionedId: String = "$id${version?.let { "#$it" } ?: ""}"

    fun isComplete() =
        isAuthorized && (endDate == null || startDate.isBefore(endDate)) && serviceUserId != null
}
