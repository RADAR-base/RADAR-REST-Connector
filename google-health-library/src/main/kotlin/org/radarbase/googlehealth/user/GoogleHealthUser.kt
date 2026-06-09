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
import org.radarcns.kafka.ObservationKey
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleHealthUser(
    @param:JsonProperty("id") override val id: String,
    @param:JsonProperty("createdAt") override val createdAt: Instant,
    @param:JsonProperty("projectId") override val projectId: String,
    @param:JsonProperty("userId") override val userId: String,
    @param:JsonProperty("humanReadableUserId") override val humanReadableUserId: String?,
    @param:JsonProperty("sourceId") override val sourceId: String,
    @param:JsonProperty("externalId") override val externalId: String?,
    @param:JsonProperty("isAuthorized") override val isAuthorized: Boolean,
    @param:JsonProperty("startDate") override val startDate: Instant,
    @param:JsonProperty("endDate") override val endDate: Instant?,
    @param:JsonProperty("version") override val version: String? = null,
    @param:JsonProperty("serviceUserId") override val serviceUserId: String?,
) : User {
    override val observationKey: ObservationKey = ObservationKey(projectId, userId, sourceId)
    override val versionedId: String = "$id${version?.let { "#$it" } ?: ""}"
}
