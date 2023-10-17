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

class OuraRequestGenerator @JvmOverloads
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

    override fun requests(user: User, max: Int): Sequence<RestRequest> {
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

    override fun requests(route: Route, max: Int): Sequence<RestRequest> {
        return userRepository
            .stream()
            .flatMap { user ->
                if (user.ready()) {
                    return@flatMap generateRequests(route, user)
                } else {
                    emptySequence()
                }
            }
            .takeWhile { !shouldBackoff }
    }

    override fun requests(route: Route, user: User, max: Int): Sequence<RestRequest> {
        return if (user.ready()) {
            return generateRequests(route, user)
        } else {
            emptySequence()
        }
    }

    fun generateRequests(route: Route, user: User): Sequence<RestRequest> {
        val offset = ouraOffsetManager.getOffset(route, user)
        val startDate = user.startDate
        val startOffset: Instant = if (offset == null) {
            logger.debug("No offsets found for $user, using the start date.")
            startDate
        } else {
            logger.debug("Offsets found in persistence.")
            offset.offset.coerceAtLeast(startDate)
        }
        val endDate = user.endDate
        if (endDate <= startOffset) return emptySequence()
        val endTime = (startOffset + defaultQueryRange).coerceAtMost(endDate)
        return route.generateRequests(user, startOffset, endTime)
    }

    fun handleResponse(req: RestRequest, response: Response): OuraResult<List<TopicData>> {
        if (response.isSuccessful) {
            return OuraResult.Success<List<TopicData>>(requestSuccessful(req, response))
        } else {
            try {
                OuraResult.Error(requestFailed(req, response))
            } catch (e: TooManyRequestsException) {} finally {
                return OuraResult.Success(listOf<TopicData>())
            }
        }
    }

    override fun requestSuccessful(request: RestRequest, response: Response): List<TopicData> {
        logger.debug("Request successful: {}. Writing to offsets...", request.request)
        val body: ResponseBody? = response.body
        val data = body?.bytes()!!
        val records = request.route.converters.flatMap {
            it.convert(request, response.headers, data)
        }
        ouraOffsetManager.updateOffsets(request.route, request.user, request.endDate)
        return records
    }

    override fun requestFailed(request: RestRequest, response: Response): OuraError {
        return when (response.code) {
            429 -> {
                logger.info("Too many requests, rate limit reached. Backing off...")
                nextRequestTime = Instant.now() + BACK_OFF_TIME
                OuraRateLimitError("Rate limit reached..", TooManyRequestsException(), "429")
            }
            403 -> {
                logger.warn(
                    "User ${request.user} has expired." +
                        "Please renew the subscription...",
                )
                userNextRequest[request.user.versionedId] = Instant.now().plus(USER_BACK_OFF_TIME)
                OuraAccessForbiddenError(
                    "Oura subscription has expired or API data not available..",
                    IOException("Unauthorized"),
                    "403",
                )
            }
            401 -> {
                logger.warn(
                    "User ${request.user} access token is" +
                        " expired, malformed, or revoked. " + response.body?.string(),
                )
                userNextRequest[request.user.versionedId] = Instant.now().plus(USER_BACK_OFF_TIME)
                OuraUnauthorizedAccessError(
                    "Access token expired or revoked..",
                    IOException("Unauthorized"),
                    "401",
                )
            }
            400 -> {
                logger.warn("Client exception..")
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
                OuraNotFoundError(response.body!!.string(), IOException("Data not found"), "404")
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
        private val BACK_OFF_TIME = Duration.ofMinutes(1L)
        private val USER_BACK_OFF_TIME = Duration.ofDays(1L)
        val JSON_FACTORY = JsonFactory()
        val JSON_READER = ObjectMapper(JSON_FACTORY).registerModule(JavaTimeModule()).reader()
    }
}
