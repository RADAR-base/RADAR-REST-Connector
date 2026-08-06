/*
 * Copyright 2026 Onsentia
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
package org.radarbase.connect.rest.huawei.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.radarbase.huawei.user.User
import org.radarcns.kafka.ObservationKey
import java.time.Instant

/**
 * A single user's Huawei Health Kit credentials, read from (and written back to) a local YAML
 * file by [HuaweiYamlUserRepository]. Mirrors Fitbit's `LocalUser`. See
 * `docker/huawei-user.yml.template` for the expected file format.
 *
 * @author yatharthranjan
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
class HuaweiLocalUser : User {
    @JsonProperty("id")
    override var id: String = ""

    @JsonProperty("projectId")
    override var projectId: String = ""

    @JsonProperty("userId")
    override var userId: String = ""

    @JsonProperty("sourceId")
    override var sourceId: String = ""

    @JsonProperty("externalUserId")
    override var externalId: String? = null

    @JsonProperty("startDate")
    override var startDate: Instant = Instant.parse("2017-01-01T00:00:00Z")

    @JsonProperty("endDate")
    override var endDate: Instant? = Instant.parse("9999-12-31T23:59:59.999Z")

    @JsonProperty("createdAt")
    override var createdAt: Instant = Instant.now()

    @JsonProperty("humanReadableUserId")
    override var humanReadableUserId: String? = null

    @JsonProperty("serviceUserId")
    override var serviceUserId: String? = null

    @JsonProperty("version")
    override var version: String? = null

    @JsonProperty("oauth2")
    var oauth2Credentials: OAuth2UserCredentials = OAuth2UserCredentials()

    @JsonProperty("authorized")
    var isAuthorizedOverride: Boolean? = null

    override val isAuthorized: Boolean
        get() = isAuthorizedOverride
            ?: (!oauth2Credentials.isAccessTokenExpired || oauth2Credentials.hasRefreshToken())

    override val observationKey: ObservationKey
        get() = ObservationKey(projectId, userId, sourceId)

    override val versionedId: String
        get() = "$id${version?.let { "#$it" } ?: ""}"

    fun copy(): HuaweiLocalUser {
        val copy = HuaweiLocalUser()
        copy.id = id
        copy.projectId = projectId
        copy.userId = userId
        copy.sourceId = sourceId
        copy.externalId = externalId
        copy.startDate = startDate
        copy.endDate = endDate
        copy.createdAt = createdAt
        copy.humanReadableUserId = humanReadableUserId
        copy.serviceUserId = serviceUserId
        copy.version = version
        copy.oauth2Credentials = oauth2Credentials
        copy.isAuthorizedOverride = isAuthorizedOverride
        return copy
    }

    override fun toString(): String = "HuaweiLocalUser(id='$id', versionedId='$versionedId')"
}
