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

import org.radarcns.kafka.ObservationKey
import java.time.Instant

interface User {
    val id: String
    val projectId: String
    val userId: String
    val sourceId: String
    val externalId: String?
    val startDate: Instant
    val endDate: Instant?
    val createdAt: Instant
    val humanReadableUserId: String?
    val serviceUserId: String?
    val version: String?
    val isAuthorized: Boolean
    val observationKey: ObservationKey
    val versionedId: String
}
