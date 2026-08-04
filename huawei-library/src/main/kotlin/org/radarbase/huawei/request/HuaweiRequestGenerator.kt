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

package org.radarbase.huawei.request

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import okhttp3.Response
import org.radarbase.huawei.converter.TopicData
import org.radarbase.huawei.route.Route
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserRepository
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.time.Instant

/**
 * @author yatharthranjan
 */
class HuaweiRequestGenerator(
    private val userRepository: UserRepository,
    private val huaweiOffsetManager: HuaweiOffsetManager,
    val routes: List<Route>,
) : RequestGenerator {
    private val routeNextRequest: MutableMap<String, Instant> = mutableMapOf()

    var nextRequestTime: Instant = Instant.MIN

    override fun requests(user: User, max: Int): Sequence<RestRequest> =
        routes.asSequence()
            .flatMap { route ->
                if (routeReady(user, route)) {
                    generateRequests(route, user)
                } else {
                    logger.info(
                        "Skip {} for {}: route in backoff until {}",
                        route,
                        user.versionedId,
                        routeNextRequest[routeKey(route, user)],
                    )
                    emptySequence()
                }
            }

    override fun requests(route: Route, max: Int): Sequence<RestRequest> =
        userRepository.stream()
            .flatMap { user ->
                if (routeReady(user, route)) {
                    generateRequests(route, user)
                } else {
                    logger.info(
                        "Skip {} for {}: route in backoff until {}",
                        route,
                        user.versionedId,
                        routeNextRequest[routeKey(route, user)],
                    )
                    emptySequence()
                }
            }

    override fun requests(route: Route, user: User, max: Int): Sequence<RestRequest> =
        if (routeReady(user, route)) {
            generateRequests(route, user)
        } else {
            logger.info(
                "Skip {} for {}: route in backoff until {}",
                route,
                user.versionedId,
                routeNextRequest[routeKey(route, user)],
            )
            emptySequence()
        }

    fun generateRequests(route: Route, user: User): Sequence<RestRequest> {
        val offset = huaweiOffsetManager.getOffset(route, user)
        val startDate = user.startDate
        val startOffset: Instant = if (offset == null) {
            logger.info("No offsets found for $user, using the start date.")
            startDate
        } else {
            offset.offset.coerceAtLeast(startDate)
        }
        val endDate = user.endDate?.coerceAtMost(Instant.now()) ?: Instant.now()
        if (!startOffset.isBefore(endDate)) {
            logger.info(
                "Skip {} for {}: interval empty (startOffset={} >= endDate={})",
                route,
                user.versionedId,
                startOffset,
                endDate,
            )
            return emptySequence()
        }
        return route.generateRequests(user, startOffset, endDate, USER_MAX_REQUESTS)
    }

    fun handleResponse(req: RestRequest, response: Response): HuaweiResult<List<TopicData>> {
        return if (response.isSuccessful) {
            HuaweiResult.Success(requestSuccessful(req, response))
        } else {
            try {
                HuaweiResult.Error(requestFailed(req, response))
            } catch (e: TooManyRequestsException) {
                HuaweiResult.Success(emptyList())
            }
        }
    }

    override fun requestSuccessful(request: RestRequest, response: Response): List<TopicData> {
        logger.debug("Request successful: {}..", request.request)
        val body = response.body
        val data = body?.bytes() ?: ByteArray(0)
        val records = request.route.converters.flatMap {
            it.convert(
                request,
                response.headers,
                data,
            )
        }
        val offset = records.maxByOrNull { it.offset }?.offset
        val key = routeKey(request.route, request.user)
        if (offset != null) {
            val maxOffsetTime = Instant.ofEpochSecond(offset)
            val nextOffset = maxOffsetTime.plus(OFFSET_BUFFER).coerceAtLeast(request.endDate)
            huaweiOffsetManager.updateOffsets(request.route, request.user, nextOffset)
        } else {
            huaweiOffsetManager.updateOffsets(request.route, request.user, request.endDate)
        }
        routeNextRequest[key] = Instant.now().plus(SUCCESS_BACK_OFF_TIME)
        return records
    }

    override fun requestFailed(request: RestRequest, response: Response): HuaweiError {
        val key = routeKey(request.route, request.user)
        return when (response.code) {
            429 -> {
                logger.info("Too many requests, rate limit reached. Backing off...")
                nextRequestTime = Instant.now() + BACK_OFF_TIME
                routeNextRequest[key] = Instant.now().plus(BACK_OFF_TIME)
                HuaweiRateLimitError("Rate limit reached..", TooManyRequestsException(), "429")
            }
            403 -> {
                val body = response.body?.string() ?: "no response body"
                logger.warn(
                    "User {} does not have access to this Huawei Health Kit data type: {}",
                    request.user,
                    body,
                )
                routeNextRequest[key] = Instant.now().plus(USER_BACK_OFF_TIME)
                HuaweiAccessForbiddenError(
                    "Huawei Health Kit scope not granted or data not available: $body",
                    IOException("Forbidden"),
                    "403",
                )
            }
            401 -> {
                val body = response.body?.string() ?: "no response body"
                logger.warn(
                    "User {} access token is expired, malformed, or revoked: {}",
                    request.user,
                    body,
                )
                routeNextRequest[key] = Instant.now().plus(USER_BACK_OFF_TIME)
                HuaweiUnauthorizedAccessError(
                    "Access token expired or revoked: $body",
                    IOException("Unauthorized"),
                    "401",
                )
            }
            400 -> {
                val body = response.body?.string() ?: "no response body"
                logger.warn("Client exception for request {}: {}", request, body)
                routeNextRequest[key] = Instant.now().plus(BACK_OFF_TIME)
                HuaweiClientException(
                    "Client unsupported or unauthorized: $body",
                    IOException("Invalid client"),
                    "400",
                )
            }
            422 -> {
                logger.warn("Request failed (validation error): {}, {}", request, response)
                routeNextRequest[key] = Instant.now().plus(BACK_OFF_TIME)
                HuaweiValidationError(
                    response.body?.string() ?: "validation error",
                    IOException("Validation error"),
                    "422",
                )
            }
            404 -> {
                logger.warn("Not found: {}", request)
                routeNextRequest[key] = Instant.now().plus(BACK_OFF_TIME)
                HuaweiNotFoundError(
                    response.body?.string() ?: "not found",
                    IOException("Data not found"),
                    "404",
                )
            }
            else -> {
                val body = response.body?.string() ?: "unknown error"
                logger.warn("Request failed: {}: {}", request, body)
                routeNextRequest[key] = Instant.now().plus(BACK_OFF_TIME)
                HuaweiGenericError(
                    body,
                    IOException("Unknown error"),
                    response.code.toString(),
                )
            }
        }
    }

    private fun routeReady(user: User, route: Route): Boolean {
        val key = routeKey(route, user)
        return routeNextRequest[key]?.let { Instant.now() > it } ?: true
    }

    private fun routeKey(route: Route, user: User): String = user.versionedId + "#" + route

    companion object {
        private val logger = LoggerFactory.getLogger(HuaweiRequestGenerator::class.java)
        private val BACK_OFF_TIME = Duration.ofMinutes(10L)
        private val USER_BACK_OFF_TIME = Duration.ofHours(12L)
        private val SUCCESS_BACK_OFF_TIME = Duration.ofSeconds(10L)
        private val OFFSET_BUFFER = Duration.ofHours(1)
        private const val USER_MAX_REQUESTS = 1000
        val JSON_FACTORY = JsonFactory()
        val JSON_READER = ObjectMapper(JSON_FACTORY).registerModule(JavaTimeModule()).reader()
    }
}
