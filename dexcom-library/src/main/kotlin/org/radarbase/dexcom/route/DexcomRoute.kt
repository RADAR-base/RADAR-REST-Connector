package org.radarbase.dexcom.route

import okhttp3.Request
import org.radarbase.dexcom.request.RestRequest
import org.radarbase.dexcom.user.User
import org.radarbase.dexcom.user.UserRepository
import java.time.Duration

abstract class DexcomRoute(
    private val userRepository: UserRepository,
    override val maxIntervalPerRequest: Duration = DEFAULT_INTERVAL_PER_REQUEST,
) : Route {

    fun createRequest(user: User, baseUrl: String, queryParams: String): Request {
        val accessToken = userRepository.getAccessToken(user)
        val request =
            Request.Builder()
                .url(baseUrl + queryParams)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build()

        return request
    }

    companion object {
        private val DEFAULT_INTERVAL_PER_REQUEST = Duration.ofDays(30L)
    }
}
