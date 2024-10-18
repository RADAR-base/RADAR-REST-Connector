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
    private val userNextRequest: MutableMap<String, Instant> = mutableMapOf()

    public var nextRequestTime: Instant = Instant.MIN

    private val shouldBackoff: Boolean
        get() = Instant.now() < nextRequestTime

    override fun requests(
        user: User,
        max: Int,
    ): Sequence<RestRequest> {
        return if (user.ready()) {
            routes.asSequence()
                .flatMap { route ->
                    return@flatMap generateRequests(route, user)
                }
                .takeWhile { !shouldBackoff }
        } else {
            emptySequence()
        }
    }

    override fun requests(
        route: Route,
        max: Int,
    ): Sequence<RestRequest> {
        return userRepository
            .stream()
            .flatMap { user ->
                if (user.ready()) {
                    generateRequests(route, user)
                } else {
                    emptySequence()
                }
            }
            .takeWhile { !shouldBackoff }
    }

    override fun requests(
        route: Route,
        user: User,
        max: Int,
    ): Sequence<RestRequest> {
        return if (user.ready()) {
            return generateRequests(route, user).takeWhile { !shouldBackoff }
        } else {
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
        if (Duration.between(startOffset, endDate) <= ONE_DAY) {
            logger.info("Interval between dates is too short. Not requesting..")
            return emptySequence()
        }
        val endTime = (startOffset + defaultQueryRange).coerceAtMost(endDate)
        return route.generateRequests(user, startOffset, endTime, USER_MAX_REQUESTS)
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
            ouraOffsetManager.updateOffsets(
                request.route,
                request.user,
                Instant.ofEpochSecond(offset).plus(ONE_DAY),
            )
            val nextRequestTime = Instant.now().plus(SUCCESS_BACK_OFF_TIME)
            userNextRequest[request.user.versionedId] =
                userNextRequest[request.user.versionedId]?.let {
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
                userNextRequest[request.user.versionedId] = Instant.now().plus(BACK_OFF_TIME)
            } else {
                userNextRequest[request.user.versionedId] = Instant.now().plus(BACK_OFF_TIME)
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
                userNextRequest[request.user.versionedId] =
                    Instant.now()
                        .plus(
                            USER_BACK_OFF_TIME,
                        )
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
                userNextRequest[request.user.versionedId] =
                    Instant.now()
                        .plus(
                            USER_BACK_OFF_TIME,
                        )
                OuraUnauthorizedAccessError(
                    "Access token expired or revoked..",
                    IOException("Unauthorized"),
                    "401",
                )
            }
            400 -> {
                logger.warn("Client exception..")
                nextRequestTime = Instant.now() + BACK_OFF_TIME
                OuraClientException(
                    "Client unsupported or unauthorized..",
                    IOException("Invalid client"),
                    "400",
                )
            }
            422 -> {
                logger.warn("Request Failed: {}, {}", request, response)
                OuraValidationError(
                    response.body!!.string(),
                    IOException("Validation error"),
                    "422",
                )
            }
            404 -> {
                logger.warn("Not found..")
                OuraNotFoundError(
                    response.body!!.string(),
                    IOException("Data not found"),
                    "404",
                )
            }
            else -> {
                logger.warn("Request Failed: {}, {}", request, response)
                OuraGenericError(response.body!!.string(), IOException("Unknown error"), "500")
            }
        }
    }

    private fun User.ready(): Boolean {
        return if (versionedId in userNextRequest) {
            Instant.now() > userNextRequest[versionedId]
        } else {
            true
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OuraRequestGenerator::class.java)
        private val BACK_OFF_TIME = Duration.ofMinutes(10L)
        private val ONE_DAY = Duration.ofDays(1L)
        private val TIME_AFTER_REQUEST = Duration.ofDays(30)
        private val USER_BACK_OFF_TIME = Duration.ofHours(12L)
        private val SUCCESS_BACK_OFF_TIME = Duration.ofMinutes(1L)
        private val USER_MAX_REQUESTS = 20
        val JSON_FACTORY = JsonFactory()
        val JSON_READER = ObjectMapper(JSON_FACTORY).registerModule(JavaTimeModule()).reader()
    }
}
