/*
 * Copyright 2026 King's College London
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.googlehealth.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * The user list returned by the RADAR Rest Source Authorizer. Each entry is read with the
 * authorizer's own nullability, and only complete users end up in [users]. One incomplete
 * entry is logged and skipped instead of failing the whole list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class GoogleHealthUsers(@JsonProperty("users") entries: List<RestSourceUserDTO>) {
    val users: List<GoogleHealthUser> = entries.mapNotNull { it.toUserOrNull() }
}

/** A user as the authorizer's `RestSourceUserDTO` declares it, where most fields are nullable. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RestSourceUserDTO(
    @param:JsonProperty("id") val id: String?,
    @param:JsonProperty("createdAt") val createdAt: Instant?,
    @param:JsonProperty("projectId") val projectId: String?,
    @param:JsonProperty("userId") val userId: String?,
    @param:JsonProperty("humanReadableUserId") val humanReadableUserId: String?,
    @param:JsonProperty("sourceId") val sourceId: String?,
    @param:JsonProperty("externalId") val externalId: String?,
    @param:JsonProperty("isAuthorized") val isAuthorized: Boolean = false,
    @param:JsonProperty("startDate") val startDate: Instant?,
    @param:JsonProperty("endDate") val endDate: Instant?,
    @param:JsonProperty("version") val version: String? = null,
    @param:JsonProperty("serviceUserId") val serviceUserId: String?,
) {
    /** Returns this user as a [GoogleHealthUser], or null if a required field is missing. */
    fun toUserOrNull(): GoogleHealthUser? {
        if (id == null || createdAt == null || projectId == null || userId == null ||
            sourceId == null || startDate == null || serviceUserId == null
        ) {
            logger.warn("Skipping incomplete Google Health user id={} from the authorizer", id)
            return null
        }
        return GoogleHealthUser(
            id = id,
            createdAt = createdAt,
            projectId = projectId,
            userId = userId,
            humanReadableUserId = humanReadableUserId,
            sourceId = sourceId,
            externalId = externalId,
            isAuthorized = isAuthorized,
            startDate = startDate,
            endDate = endDate,
            version = version,
            serviceUserId = serviceUserId,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RestSourceUserDTO::class.java)
    }
}
