package org.radarbase.dexcom.route

import org.radarbase.dexcom.request.RestRequest
import org.radarbase.dexcom.user.User
import org.radarbase.dexcom.user.UserRepository
import java.time.Instant

class DexcomEGVRoute(
    userRepository: UserRepository,
) : DexcomRoute(userRepository) {
    override fun toString(): String = "egv"

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
    ): Sequence<RestRequest> = emptySequence()

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
        max: Int,
    ): Sequence<RestRequest> = emptySequence()
}
