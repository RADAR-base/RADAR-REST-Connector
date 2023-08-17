package org.radarbase.oura.request

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.radarbase.oura.route.OuraDailySleepRoute
import org.radarbase.oura.route.OuraRouteFactory
import org.radarbase.oura.route.Route
import org.radarbase.oura.user.User
import org.radarbase.oura.user.UserRepository
import org.slf4j.LoggerFactory
import org.apache.avro.specific.SpecificRecord
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.streams.asSequence

class OuraRequestGenerator(
    // Insert offset managers, user repositories, configs here
    private val userRepository: UserRepository,
    private val defaultQueryRange: Duration = Duration.ofDays(15),
    private val ouraOffsetManager: OuraOffsetManager,
) : RequestGenerator {

    var client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val routes: List<Route> =
        listOf(
            OuraRouteFactory.getRoute(),
        )

    private val userNextRequest: MutableMap<String, Instant> = mutableMapOf()

    private var nextRequestTime: Instant = Instant.MIN

    private val shouldBackoff: Boolean
        get() = Instant.now() < nextRequestTime

    override fun requests(user: User, max: Int): Sequence<RestRequest> {
        return if (user.ready()) {
            routes.asSequence()
                .flatMap { route ->
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
                    if (endDate <= startOffset) return@flatMap emptySequence()
                    val endTime = (startOffset + defaultQueryRange).coerceAtMost(endDate)
                    route.generateRequests(user, startOffset, endTime)
                }
                .takeWhile { !shouldBackoff }
        } else {
            emptySequence()
        }
    }

    override fun requests(route: Route, max: Int): Sequence<RestRequest> {
        return userRepository
            .stream()
            .asSequence()
            .flatMap { user ->
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
                if (endDate <= startOffset) return@flatMap emptySequence()
                val endTime = (startOffset + defaultQueryRange).coerceAtMost(endDate)
                route.generateRequests(user, startOffset, endTime)
            }
            .takeWhile { !shouldBackoff }
    }

    override fun requests(route: Route, user: User, max: Int): Sequence<RestRequest> {
        return if (user.ready()) {
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
        } else {
            emptySequence()
        }
    }

    fun makeRequest(req: RestRequest) {
        logger.debug("Making Request: {}", req.request)
        try {
            client.newCall(req.request).execute().use { response ->
                if (response.isSuccessful) {
                    requestSuccessful(req, response)
                } else {
                    requestFailed(req, response)
                }
            }
        } catch (ex: Throwable) {
            logger.warn("Error making request ${req.request.url}.", ex)
        }
    }

    override fun requestSuccessful(request: RestRequest, response: Response): List<Pair<SpecificRecord, SpecificRecord>> {
        logger.debug("Request successful: {}. Writing to offsets...", request.request)
        val body: ResponseBody? = response.body
        val records = request.route.converter.convert(request, response.headers, body?.bytes()!!)
        ouraOffsetManager.updateOffsets(request.route, request.user, request.endDate)
        return records
    }

    override fun requestFailed(request: RestRequest, response: Response) {
        when (response.code) {
            429 -> {
                logger.info("Too many requests, rate limit reached. Backing off...")
                nextRequestTime = Instant.now() + BACK_OFF_TIME
                throw TooManyRequestsException()
            }
            409 -> {
                logger.info("A duplicate request was made. Marking successful...")
                requestSuccessful(request, response)
            }
            412 -> {
                logger.warn(
                    "User ${request.user} does not have correct permissions/scopes enabled. " +
                        "Please enable in app. User backing off for $USER_BACK_OFF_TIME...",
                )
                userNextRequest[request.user.versionedId] = Instant.now().plus(USER_BACK_OFF_TIME)
            }
            else -> logger.warn("Request Failed: {}, {}", request, response)
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
