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
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import okhttp3.Response
import org.radarbase.huawei.converter.TopicData
import org.radarbase.huawei.route.Route
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserNotAuthorizedException
import org.radarbase.huawei.user.UserRepository
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * @author yatharthranjan
 */
class HuaweiRequestGenerator(
    private val userRepository: UserRepository,
    private val huaweiOffsetManager: HuaweiOffsetManager,
    val routes: List<Route>,
) : RequestGenerator {
    private val routeNextRequest: MutableMap<String, Instant> = ConcurrentHashMap()

    /** Routes whose last request failed, until when. Stops the rest of a lazily generated
     * request sequence for that route and user, so a later chunk can't advance the offset past
     * a chunk that failed. */
    private val routeFailedUntil: MutableMap<String, Instant> = ConcurrentHashMap()

    var nextRequestTime: Instant = Instant.MIN

    override fun requests(user: User, max: Int): Sequence<RestRequest> =
        routes.asSequence()
            .flatMap { route -> requests(route, user, max) }

    override fun requests(route: Route, max: Int): Sequence<RestRequest> =
        userRepository.stream()
            .flatMap { user -> requests(route, user, max) }

    override fun requests(route: Route, user: User, max: Int): Sequence<RestRequest> =
        if (routeReady(user, route)) {
            generateRequests(route, user)
        } else {
            logger.debug(
                "Skip {} for {}: route in backoff until {}",
                route,
                user.versionedId,
                routeNextRequest[routeKey(route, user)],
            )
            emptySequence()
        }

    fun generateRequests(route: Route, user: User): Sequence<RestRequest> {
        val startOffset = currentOffset(route, user)
        val endDate = user.endDate?.coerceAtMost(Instant.now()) ?: Instant.now()
        if (!startOffset.isBefore(endDate)) {
            logger.debug(
                "Skip {} for {}: interval empty (startOffset={} >= endDate={})",
                route,
                user.versionedId,
                startOffset,
                endDate,
            )
            return emptySequence()
        }
        val key = routeKey(route, user)
        return route.generateRequests(user, startOffset, endDate, USER_MAX_REQUESTS)
            .guarded(route, user)
            .takeWhile { !isBlocked(key) }
    }

    /**
     * Requests are built lazily, and building one fetches the user's access token, which throws
     * if the user is no longer authorized or the token endpoint is unreachable. Contain that to
     * this route and user (backing it off) instead of letting it escape the source task's poll.
     */
    private fun Sequence<RestRequest>.guarded(route: Route, user: User): Sequence<RestRequest> {
        val source = this
        return sequence {
            val iterator = source.iterator()
            while (true) {
                val next = try {
                    if (!iterator.hasNext()) break
                    iterator.next()
                } catch (ex: UserNotAuthorizedException) {
                    logger.warn("User {} is not authorized: {}", user.versionedId, ex.message)
                    backOff(route, user, USER_BACK_OFF_TIME)
                    break
                } catch (ex: Exception) {
                    logger.warn(
                        "Failed to create {} request for {}: {}",
                        route,
                        user.versionedId,
                        ex.toString(),
                    )
                    backOff(route, user, BACK_OFF_TIME)
                    break
                }
                yield(next)
            }
        }
    }

    private fun currentOffset(route: Route, user: User): Instant {
        val offset = huaweiOffsetManager.getOffset(route, user)
        return if (offset == null) {
            logger.info("No offsets found for {} on {}, using the start date.", user, route)
            user.startDate
        } else {
            offset.offset.coerceAtLeast(user.startDate)
        }
    }

    fun handleResponse(req: RestRequest, response: Response): HuaweiResult<List<TopicData>> {
        if (response.code == 400 && response.peekBody(PEEK_BYTES).string().contains(NO_COLLECTOR)) {
            // Huawei's way of saying the user has no data source (so no data) for this type.
            logger.debug("No data source of {} for {}", req.route, req.user.versionedId)
            return HuaweiResult.Success(recordsReceived(req, MissingNode.getInstance()))
        }
        return if (response.isSuccessful) {
            try {
                HuaweiResult.Success(requestSuccessful(req, response))
            } catch (ex: IOException) {
                // Unreadable or malformed body: don't advance the offset, and retry later.
                logger.warn("Failed to read response of {}: {}", req, ex.toString())
                backOff(req.route, req.user, BACK_OFF_TIME)
                HuaweiResult.Success(emptyList())
            }
        } else {
            try {
                HuaweiResult.Error(requestFailed(req, response))
            } catch (e: TooManyRequestsException) {
                HuaweiResult.Success(emptyList())
            }
        }
    }

    /**
     * Converts the response and advances the route's offset.
     *
     * Huawei devices sync to the Huawei cloud with a delay of minutes to days, so data for a
     * period can appear after that period was already queried. The offset is therefore never
     * advanced past [LATE_SYNC_WINDOW] ago on the basis of an empty response: only past the
     * latest record actually received. Records starting before the current offset were already
     * emitted (the offset sits just past the latest one) and are dropped, since Huawei also
     * returns samples that merely overlap the queried window.
     */
    override fun requestSuccessful(request: RestRequest, response: Response): List<TopicData> {
        logger.debug("Request successful: {}..", request.request)
        val data = response.body?.bytes() ?: ByteArray(0)
        val root: JsonNode = if (data.isEmpty()) {
            MissingNode.getInstance()
        } else {
            JSON_READER.readTree(data) ?: MissingNode.getInstance()
        }
        return recordsReceived(request, root)
    }

    private fun recordsReceived(request: RestRequest, root: JsonNode): List<TopicData> {
        val now = Instant.now()
        val currentOffset = currentOffset(request.route, request.user)
        val records = request.route.converters
            .flatMap { it.convert(request, root) }
            .filter { it.offset >= currentOffset.epochSecond }

        val settledEnd = request.endDate.coerceAtMost(now.minus(LATE_SYNC_WINDOW))
        val maxOffset = records.maxOfOrNull { it.offset }
        val nextOffset = if (maxOffset != null) {
            val afterLatest = Instant.ofEpochSecond(maxOffset + 1)
            if (root.path("hasMoreData").asBoolean(false)) {
                // Only part of this window was returned; continue right after the latest record.
                logger.info(
                    "More {} data available for {} than returned; continuing after {}",
                    request.route,
                    request.user.versionedId,
                    afterLatest,
                )
                afterLatest
            } else {
                afterLatest.coerceAtLeast(settledEnd)
            }
        } else {
            settledEnd.coerceAtLeast(currentOffset)
        }
        if (nextOffset.isAfter(currentOffset)) {
            huaweiOffsetManager.updateOffsets(request.route, request.user, nextOffset)
        }

        val key = routeKey(request.route, request.user)
        routeFailedUntil -= key
        routeNextRequest[key] = if (records.isEmpty() && request.endDate > settledEnd) {
            // Caught up to data that may still be syncing: check again later.
            now.plus(CAUGHT_UP_BACK_OFF_TIME)
        } else {
            now.plus(SUCCESS_BACK_OFF_TIME)
        }
        return records
    }

    override fun requestFailed(request: RestRequest, response: Response): HuaweiError {
        return when (response.code) {
            429 -> {
                logger.info(
                    "Too many requests, rate limit reached. Backing off... {}",
                    response.body?.string(),
                )
                nextRequestTime = Instant.now() + BACK_OFF_TIME
                backOff(request.route, request.user, BACK_OFF_TIME)
                HuaweiRateLimitError("Rate limit reached..", TooManyRequestsException(), "429")
            }
            403 -> {
                val body = response.body?.string() ?: "no response body"
                logger.warn(
                    "User {} does not have access to this Huawei Health Kit data type: {}",
                    request.user,
                    body,
                )
                backOff(request.route, request.user, USER_BACK_OFF_TIME)
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
                // Refetch the token on the next attempt; a user whose refresh token is also no
                // longer valid is then backed off when building that request.
                userRepository.invalidateAccessToken(request.user)
                backOff(request.route, request.user, BACK_OFF_TIME)
                HuaweiUnauthorizedAccessError(
                    "Access token expired or revoked: $body",
                    IOException("Unauthorized"),
                    "401",
                )
            }
            400 -> {
                val body = response.body?.string() ?: "no response body"
                logger.warn("Client exception for request {}: {}", request, body)
                // An unknown data type name won't become valid by retrying soon.
                val permanent = body.contains(INVALID_DATA_TYPE)
                backOff(
                    request.route,
                    request.user,
                    if (permanent) USER_BACK_OFF_TIME else BACK_OFF_TIME,
                )
                HuaweiClientException(
                    "Client unsupported or unauthorized: $body",
                    IOException("Invalid client"),
                    "400",
                )
            }
            422 -> {
                logger.warn("Request failed (validation error): {}, {}", request, response)
                backOff(request.route, request.user, BACK_OFF_TIME)
                HuaweiValidationError(
                    response.body?.string() ?: "validation error",
                    IOException("Validation error"),
                    "422",
                )
            }
            404 -> {
                logger.warn("Not found: {}", request)
                backOff(request.route, request.user, BACK_OFF_TIME)
                HuaweiNotFoundError(
                    response.body?.string() ?: "not found",
                    IOException("Data not found"),
                    "404",
                )
            }
            else -> {
                val body = response.body?.string() ?: "unknown error"
                logger.warn("Request failed: {}: {}", request, body)
                backOff(request.route, request.user, BACK_OFF_TIME)
                HuaweiGenericError(
                    body,
                    IOException("Unknown error"),
                    response.code.toString(),
                )
            }
        }
    }

    private fun backOff(route: Route, user: User, duration: Duration) {
        val key = routeKey(route, user)
        val until = Instant.now().plus(duration)
        routeNextRequest[key] = until
        routeFailedUntil[key] = until
    }

    private fun routeReady(user: User, route: Route): Boolean {
        val now = Instant.now()
        val key = routeKey(route, user)
        return now > nextRequestTime && routeNextRequest[key]?.let { now > it } ?: true
    }

    /** Whether requests for this route and user must stop, after a failure or rate limit. */
    private fun isBlocked(key: String): Boolean {
        val now = Instant.now()
        return now <= nextRequestTime || routeFailedUntil[key]?.let { now <= it } ?: false
    }

    private fun routeKey(route: Route, user: User): String = user.versionedId + "#" + route

    companion object {
        private val logger = LoggerFactory.getLogger(HuaweiRequestGenerator::class.java)
        private val BACK_OFF_TIME = Duration.ofMinutes(10L)
        private val USER_BACK_OFF_TIME = Duration.ofHours(12L)
        private val SUCCESS_BACK_OFF_TIME = Duration.ofSeconds(10L)
        private val CAUGHT_UP_BACK_OFF_TIME = Duration.ofMinutes(30L)

        /** How long after the fact Huawei data may still be synced to the cloud. */
        private val LATE_SYNC_WINDOW = Duration.ofDays(7L)
        private const val USER_MAX_REQUESTS = 1000
        private const val PEEK_BYTES = 64L * 1024L
        private const val NO_COLLECTOR = "no default dataCollector found"
        private const val INVALID_DATA_TYPE = "Invalid dataTypeName"
        val JSON_FACTORY = JsonFactory()
        val JSON_READER = ObjectMapper(JSON_FACTORY).registerModule(JavaTimeModule()).reader()
    }
}
