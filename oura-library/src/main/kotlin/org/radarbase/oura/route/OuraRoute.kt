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

import okhttp3.HttpUrl
import okhttp3.Request
import org.radarbase.oura.request.RestRequest
import org.radarbase.oura.user.User
import java.time.Duration
import java.time.Instant

abstract class OuraRoute(
        // Add back user repository here
) : Route {
        val maxIntervalPerRequest: Duration
        get() = DEFAULT_INTERVAL_PER_REQUEST

    fun createRequest(user: User, baseUrl: String, queryParams: String): Request {
        val request = Request.Builder()
                .url(baseUrl + queryParams)
                .header("Authorization", "Bearer hi")
                .get()
                .build()

        return request
    }

    override fun generateRequests(
            user: User,
            start: Instant,
            end: Instant,
            max: Int
    ): Sequence<RestRequest> {
        val maxIntervalPerRequest = Duration.ofDays(0)
        val startRange = start
        val endRange = (startRange + maxIntervalPerRequest).coerceAtMost(end)
        val request = createRequest(
                user, "$OURA_API_BASE_URL/${subPath()}",
                "?start_date=${startRange.epochSecond}" +
                        "&end_date=${endRange.epochSecond}"
        )
        return sequenceOf(RestRequest(request, user, this, startRange, endRange))
    }

    override fun maxBackfillPeriod(): Duration {
        // 2 years default. Activity API routes will override this with 5 years
        return Duration.ofDays(365 * 5)
    }

    abstract fun subPath(): String

    companion object {
        const val OURA_API_BASE_URL =
                "https://api.ouraring.com/v2/usercollection/"
        const val ROUTE_METHOD = "GET"
        private val DEFAULT_INTERVAL_PER_REQUEST = Duration.ofDays(5L)
    }
}