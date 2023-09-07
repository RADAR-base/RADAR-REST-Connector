package org.radarbase.oura.route

import org.radarbase.oura.request.RestRequest
import org.radarbase.oura.user.User
import java.time.Duration
import java.time.Instant

interface Route {

    fun generateRequests(user: User, start: Instant, end: Instant): Sequence<RestRequest>

    fun generateRequests(user: User, start: Instant, end: Instant, max: Int): Sequence<RestRequest>

    /**
     * This is how it would appear in the offsets
     */
    override fun toString(): String

    /**
     * The number of days to request in a single request of this route.
     */
    val maxIntervalPerRequest: Duration
}
