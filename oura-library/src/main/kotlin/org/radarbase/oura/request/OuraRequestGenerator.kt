package org.radarbase.oura.request

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import okhttp3.Response
import okhttp3.ResponseBody
import org.radarbase.oura.converter.TopicData
import org.radarbase.oura.route.OuraRouteFactory
import org.radarbase.oura.route.Route
import org.radarbase.oura.user.User
import org.radarbase.oura.user.UserRepository
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.time.Instant
import kotlin.streams.asSequence

class OuraRequestGenerator
@JvmOverloads
constructor(
    private val userRepository: UserRepository,
    private val defaultQueryRange: Duration = Duration.ofDays(15),
    private val ouraOffsetManager: OuraOffsetManager,
    public val routes: List<Route> = OuraRouteFactory.getRoutes(userRepository),
) : RequestGenerator {
    private val routeNextRequest: MutableMap<String, Instant> = mutableMapOf()

    public var nextRequestTime: Instant = Instant.MIN

    private val shouldBackoff: Boolean
        get() = Instant.now() < nextRequestTime

    override fun requests(
        user: User,
        max: Int,
    ): Sequence<RestRequest> {
        return routes.asSequence()
            .flatMap { route ->
                if (routeReady(user, route)) {
                    return@flatMap generateRequests(route, user)
                } else {
                    logger.info(
                        "Skip {} for {}: route in backoff until {}",
                        route,
                        user.versionedId,
                        routeNextRequest[routeKey(route, user)],
                    )
                    return@flatMap emptySequence()
                }
            }
    }

    override fun requests(
        route: Route,
        max: Int,
    ): Sequence<RestRequest> {
        return userRepository
            .stream()
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
    }

    override fun requests(
        route: Route,
        user: User,
        max: Int,
    ): Sequence<RestRequest> {
        return if (routeReady(user, route)) {
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

    fun generateRequests(
        route: Route,
        user: User,
    ): Sequence<RestRequest> {
        val offset = ouraOffsetManager.getOffset(route, user)
        val startDate = user.startDate
        val startOffset: Instant =
            if (offset == null) {
                logger.info("No offsets found for $user, using the start date.")
                startDate
            } else {
                val offsetTime = offset.offset
                logger.info("Offsets found in persistence: " + offsetTime.toString())
                offsetTime.coerceAtLeast(startDate)
            }
        val endDate = user.endDate?.coerceAtMost(Instant.now()) ?: Instant.now()
        if (!startOffset.isBefore(endDate)) {
            // If the user's configured endDate is in the past (>30d ago),
            // and we've reached or surpassed it (startOffset >= endDate),
            // permanently disable future requests for this user+route.
            val userEnd = user.endDate
            if (userEnd != null && endDate == userEnd &&
                Duration.between(userEnd, Instant.now()) > Duration.ofDays(30)
            ) {
                val key = routeKey(route, user)
                routeNextRequest[key] = Instant.MAX
                logger.info(
                    "Disable future requests for {}: user={}, " +
                        "endDate={} (>30d ago), startOffset={}",
                    route,
                    user.versionedId,
                    userEnd,
                    startOffset,
                )
            }
            logger.info(
                "Skip {} for {}: interval empty (startOffset={} >= endDate={}), " +
                    "persistedOffset={}, userStartDate={}",
                route,
                user.versionedId,
                startOffset,
                endDate,
                offset?.offset,
                startDate,
            )
            return emptySequence()
        }
        val timeSinceStart = Duration.between(startOffset, Instant.now())
        return if (timeSinceStart > HISTORICAL_DATA_THRESHOLD) {
            // Historical data: use 1-year chunks without max request limit
            val endTime = (startOffset + HISTORICAL_QUERY_RANGE).coerceAtMost(endDate)
            route.generateRequests(user, startOffset, endTime)
        } else {
            // Recent data: use normal chunking with max request limit
            route.generateRequests(user, startOffset, endDate, USER_MAX_REQUESTS)
        }
    }

    fun handleResponse(
        req: RestRequest,
        response: Response,
    ): OuraResult<List<TopicData>> {
        if (response.isSuccessful) {
            return OuraResult.Success<List<TopicData>>(requestSuccessful(req, response))
        } else {
            try {
                OuraResult.Error(requestFailed(req, response))
            } catch (e: TooManyRequestsException) {
            } finally {
                return OuraResult.Success(listOf<TopicData>())
            }
        }
    }

    override fun requestSuccessful(
        request: RestRequest,
        response: Response,
    ): List<TopicData> {
        logger.debug("Request successful: {}..", request.request)
        val body: ResponseBody? = response.body
        val data = body?.bytes()!!
        val records =
            request.route.converters.flatMap { it.convert(request, response.headers, data) }
        val offset = records.maxByOrNull { it -> it.offset }?.offset
        if (offset != null) {
            logger.info("Writing ${records.size} records to offsets...")
            val maxOffsetTime = Instant.ofEpochSecond(offset)
            // For older data, offset is end date
            val dataAge = Duration.between(maxOffsetTime, Instant.now())
            val nextOffset = if (dataAge <= Duration.ofDays(7)) {
                maxOffsetTime.plus(OFFSET_BUFFER)
            } else {
                maxOf(maxOffsetTime.plus(OFFSET_BUFFER), request.endDate)
            }
            ouraOffsetManager.updateOffsets(
                request.route,
                request.user,
                nextOffset,
            )
            val nextRequestTime = Instant.now().plus(SUCCESS_BACK_OFF_TIME)
            val key = routeKey(request.route, request.user)
            routeNextRequest[key] =
                routeNextRequest[key]?.let {
                    if (it > nextRequestTime) it else nextRequestTime
                } ?: nextRequestTime
        } else {
            if (request.startDate.plus(TIME_AFTER_REQUEST).isBefore(Instant.now())) {
                logger.info("No records found, updating offsets to end date..")
                ouraOffsetManager.updateOffsets(
                    request.route,
                    request.user,
                    request.endDate,
                )
                val key = routeKey(request.route, request.user)
                routeNextRequest[key] = Instant.now().plus(SUCCESS_BACK_OFF_TIME)
            } else {
                val key = routeKey(request.route, request.user)
                routeNextRequest[key] = Instant.now().plus(BACK_OFF_TIME)
            }
        }
        return records
    }

    override fun requestFailed(
        request: RestRequest,
        response: Response,
    ): OuraError {
        return when (response.code) {
            429 -> {
                logger.info("Too many requests, rate limit reached. Backing off...")
                nextRequestTime = Instant.now() + BACK_OFF_TIME
                OuraRateLimitError("Rate limit reached..", TooManyRequestsException(), "429")
            }
            403 -> {
                logger.warn(
                    "User ${request.user} has expired." + "Please renew the subscription...",
                )
                routeNextRequest[routeKey(request.route, request.user)] =
                    Instant.now().plus(USER_BACK_OFF_TIME)
                OuraAccessForbiddenError(
                    "Oura subscription has expired or API data not available..",
                    IOException("Unauthorized"),
                    "403",
                )
            }
            401 -> {
                logger.warn(
                    "User ${request.user} access token is" +
                        " expired, malformed, or revoked. " +
                        response.body?.string(),
                )
                routeNextRequest[routeKey(request.route, request.user)] =
                    Instant.now().plus(USER_BACK_OFF_TIME)
                OuraUnauthorizedAccessError(
                    "Access token expired or revoked..",
                    IOException("Unauthorized"),
                    "401",
                )
            }
            400 -> {
                logger.warn("Client exception..")
                nextRequestTime = Instant.now() + BACK_OFF_TIME
                routeNextRequest[routeKey(request.route, request.user)] =
                    Instant.now().plus(BACK_OFF_TIME)
                OuraClientException(
                    "Client unsupported or unauthorized..",
                    IOException("Invalid client"),
                    "400",
                )
            }
            422 -> {
                logger.warn("Request Failed: {}, {}", request, response)
                routeNextRequest[routeKey(request.route, request.user)] =
                    Instant.now().plus(BACK_OFF_TIME)
                OuraValidationError(
                    response.body!!.string(),
                    IOException("Validation error"),
                    "422",
                )
            }
            404 -> {
                logger.warn("Not found..")
                routeNextRequest[routeKey(request.route, request.user)] =
                    Instant.now().plus(BACK_OFF_TIME)
                OuraNotFoundError(
                    response.body!!.string(),
                    IOException("Data not found"),
                    "404",
                )
            }
            else -> {
                logger.warn("Request Failed: {}, {}", request, response)
                routeNextRequest[routeKey(request.route, request.user)] =
                    Instant.now().plus(BACK_OFF_TIME)
                OuraGenericError(response.body!!.string(), IOException("Unknown error"), "500")
            }
        }
    }

    private fun routeReady(user: User, route: Route): Boolean {
        val key = routeKey(route, user)
        return routeNextRequest[key]?.let { Instant.now() > it } ?: true
    }

    private fun routeKey(route: Route, user: User): String = user.versionedId + "#" + route

    companion object {
        private val logger = LoggerFactory.getLogger(OuraRequestGenerator::class.java)
        private val BACK_OFF_TIME = Duration.ofMinutes(10L)
        private val ONE_DAY = Duration.ofDays(1L)
        private val TIME_AFTER_REQUEST = Duration.ofDays(30)
        private val USER_BACK_OFF_TIME = Duration.ofHours(12L)
        private val SUCCESS_BACK_OFF_TIME = Duration.ofSeconds(10L)
        private val OFFSET_BUFFER = Duration.ofHours(12)
        private val USER_MAX_REQUESTS = 1000
        private val HISTORICAL_DATA_THRESHOLD = Duration.ofDays(365L)
        private val HISTORICAL_QUERY_RANGE = Duration.ofDays(365L)
        val JSON_FACTORY = JsonFactory()
        val JSON_READER = ObjectMapper(JSON_FACTORY).registerModule(JavaTimeModule()).reader()
    }
}
