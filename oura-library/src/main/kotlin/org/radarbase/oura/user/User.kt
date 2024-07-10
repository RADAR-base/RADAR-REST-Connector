package org.radarbase.oura.user

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
