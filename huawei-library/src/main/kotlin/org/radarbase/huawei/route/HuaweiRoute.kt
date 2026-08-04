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

package org.radarbase.huawei.route

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.radarbase.huawei.converter.HuaweiDataConverter
import org.radarbase.huawei.request.RestRequest
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserRepository
import java.time.Duration
import java.time.Instant

/**
 * Base class for all Huawei Health Kit routes.
 *
 * Handles OAuth2-authorized request construction (both `GET` with query parameters and `POST`
 * with a JSON body, since the Health Kit Data API mixes both styles across its endpoints) and
 * generic time-range chunking, shared by all concrete route types.
 *
 * @author yatharthranjan
 */
abstract class HuaweiRoute(
    private val userRepository: UserRepository,
    override val maxIntervalPerRequest: Duration = DEFAULT_INTERVAL_PER_REQUEST,
) : Route {
    abstract val converters: List<HuaweiDataConverter>

    protected fun createGetRequest(
        user: User,
        path: String,
        queryParams: Map<String, String>,
        baseUrl: String = HUAWEI_API_BASE_URL,
    ): Request {
        val accessToken = userRepository.getAccessToken(user)
        val urlBuilder = "$baseUrl/$path".toHttpUrl().newBuilder()
        queryParams.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
        return Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
    }

    protected fun createPostRequest(
        user: User,
        path: String,
        jsonBody: String,
        baseUrl: String = HUAWEI_API_BASE_URL,
    ): Request {
        val accessToken = userRepository.getAccessToken(user)
        return Request.Builder()
            .url("$baseUrl/$path".toHttpUrl())
            .header("Authorization", "Bearer $accessToken")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    /**
     * Split `[start, end)` into consecutive windows of at most [maxIntervalPerRequest], capped at
     * [max] windows.
     */
    protected fun chunkedRanges(
        start: Instant,
        end: Instant,
        max: Int,
    ): Sequence<Pair<Instant, Instant>> =
        generateSequence(start) { it + maxIntervalPerRequest }
            .takeWhile { it < end }
            .take(max)
            .map { rangeStart ->
                rangeStart to (rangeStart + maxIntervalPerRequest).coerceAtMost(end)
            }

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
    ): Sequence<RestRequest> = generateRequests(user, start, end, Int.MAX_VALUE)

    companion object {
        const val HUAWEI_API_BASE_URL = "https://health-api.cloud.huawei.com/healthkit/v1"
        const val HUAWEI_API_BASE_URL_V2 = "https://health-api.cloud.huawei.com/healthkit/v2"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val DEFAULT_INTERVAL_PER_REQUEST = Duration.ofDays(30L)
    }
}
