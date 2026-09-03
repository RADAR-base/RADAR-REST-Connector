package org.radarbase.dexcom.route

import okhttp3.Request
import org.radarbase.dexcom.converter.DexcomDataConverter
import org.radarbase.dexcom.request.RestRequest
import org.radarbase.dexcom.user.User
import org.radarbase.dexcom.user.UserRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

abstract class DexcomRoute(
    private val userRepository: UserRepository,
    override val maxIntervalPerRequest: Duration = DEFAULT_INTERVAL_PER_REQUEST,
) : Route {
    abstract val converters: List<DexcomDataConverter>

    fun createRequest(user: User, baseUrl: String, queryParams: String): Request {
        val accessToken = userRepository.getAccessToken(user)
        return Request.Builder()
            .url(baseUrl + queryParams)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
    }

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
    ): Sequence<RestRequest> {
        val request = createRequest(
            user,
            "$DEXCOM_API_BASE_URL/${subPath()}",
            "?startDate=${start.toDexcomDate()}&endDate=${end.toDexcomDate()}",
        )
        return sequenceOf(RestRequest(request, user, this, start, end))
    }

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
        max: Int,
    ): Sequence<RestRequest> {
        return generateSequence(start) { it + maxIntervalPerRequest }
            .takeWhile { it < end }
            .take(max)
            .map { startRange ->
                val endRange = (startRange + maxIntervalPerRequest).coerceAtMost(end)
                val request = createRequest(
                    user,
                    "$DEXCOM_API_BASE_URL/${subPath()}",
                    "?startDate=${startRange.toDexcomDate()}&endDate=${endRange.toDexcomDate()}",
                )
                RestRequest(request, user, this, startRange, endRange)
            }
    }

    abstract fun subPath(): String

    fun Instant.toDexcomDate(): String =
        LocalDateTime.ofInstant(this, ZoneOffset.UTC).format(DEXCOM_DATE_FORMAT)

    companion object {
        const val DEXCOM_API_BASE_URL = "https://api.dexcom.com/v3/users/self"
        private val DEXCOM_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        private val DEFAULT_INTERVAL_PER_REQUEST = Duration.ofDays(30L)
    }
}
