package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraHeartRateConverter
import org.radarbase.oura.user.User
import org.radarbase.oura.user.UserRepository
import org.radarcns.connector.oura.OuraHeartRate
import org.radarcns.connector.oura.OuraHeartRateSource
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import org.radarbase.oura.request.RestRequest

class OuraHeartRateRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "heartrate"

    override fun toString(): String = "oura_heart_rate"

    override var converters = listOf(OuraHeartRateConverter())

    override var maxIntervalPerRequest = Duration.ofDays(1)

    var maxRequestsPerInterval = 1000

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
    ): Sequence<RestRequest> {
        return generateRequests(user, start, end, maxRequestsPerInterval)
    }

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
        max: Int,
    ): Sequence<RestRequest> {
        return generateSequence(start) { it + maxIntervalPerRequest }
            .takeWhile { it < end }
            .take(maxRequestsPerInterval)
            .map { startRange ->
                val endRange = (startRange + maxIntervalPerRequest).coerceAtMost(end)
                val request = createRequest(
                    user,
                    "$OURA_API_BASE_URL/${subPath()}",
                    "?start_datetime=${startRange.toLocalDate()}" +
                        "&end_datetime=${endRange.toLocalDate()}",
                )
                RestRequest(request, user, this, startRange, endRange)
            }
    }
}
