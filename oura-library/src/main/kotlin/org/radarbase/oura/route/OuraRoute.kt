/*
 * Copyright 2018 The Hyve
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
package org.radarbase.oura.route

import okhttp3.Request
import org.radarbase.oura.converter.OuraDataConverter
import org.radarbase.oura.request.RestRequest
import org.radarbase.oura.user.User
import org.radarbase.oura.user.UserRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

abstract class OuraRoute(
    private val userRepository: UserRepository,
    override val maxIntervalPerRequest: Duration = DEFAULT_INTERVAL_PER_REQUEST,
) : Route {
    abstract val converters: List<OuraDataConverter>

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

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
    ): Sequence<RestRequest> {
        val request =
            createRequest(
                user,
                "$OURA_API_BASE_URL/${subPath()}",
                "?start_date=${start.toLocalDate()}" +
                    "&end_date=${end.toLocalDate()}",
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
                    "$OURA_API_BASE_URL/${subPath()}",
                    "?start_date=${startRange.toLocalDate()}" +
                        "&end_date=${endRange.toLocalDate()}",
                )
                RestRequest(request, user, this, startRange, endRange)
            }
    }

    abstract fun subPath(): String

    fun Instant.toLocalDate() = LocalDateTime.ofInstant(this, ZoneId.systemDefault()).toLocalDate()

    companion object {
        const val OURA_API_BASE_URL = "https://api.ouraring.com/v2/usercollection"
        const val ROUTE_METHOD = "GET"
        private val DEFAULT_INTERVAL_PER_REQUEST = Duration.ofDays(30L)
    }
}
