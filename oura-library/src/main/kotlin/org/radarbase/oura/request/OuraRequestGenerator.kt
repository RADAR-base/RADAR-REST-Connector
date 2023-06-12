package org.radarbase.oura.request

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import okhttp3.Response
import org.radarbase.oura.route.OuraDailySleepRoute
import org.radarbase.oura.route.Route
import org.radarbase.oura.user.User
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class OuraRequestGenerator(
        // Insert offset managers, user repositories, configs here
        private val defaultQueryRange: Duration = Duration.ofDays(15),
) : RequestGenerator {

    val routes: List<Route> = listOf(
            OuraDailySleepRoute(),
    )

    private val userNextRequest: MutableMap<String, Instant> = mutableMapOf()

    private var nextRequestTime: Instant = Instant.MIN

    private val shouldBackoff: Boolean
        get() = Instant.now() < nextRequestTime

    override fun requests(user: User, max: Int): Sequence<RestRequest> {
        return if (user.ready()) {
            routes.asSequence()
                    .flatMap { route ->
//                        val offsets: Offsets? = offsetPersistenceFactory.read(user.versionedId)
                        val offsets = null
                        val backfillLimit = Instant.now().minus(route.maxBackfillPeriod())
//                        val startDate = userRepository.getBackfillStartDate(user)
                        val startDate = user.startDate
                        var startOffset: Instant = if (offsets == null) {
                            logger.debug("No offsets found for $user, using the start date.")
                            startDate
                        } else {
                            logger.debug("Offsets found in persistence.")
                            startDate
//                            offsets.offsetsMap.getOrDefault(
//                                    UserRoute(user.versionedId, route.toString()), startDate
//                            ).coerceAtLeast(startDate)
                        }

                        if (startOffset <= backfillLimit) {
                            // the start date is before the backfill limits
                            logger.warn(
                                    "Backfill limit exceeded for $user and $route. " +
                                            "Resetting to earliest allowed start offset."
                            )
                            startOffset = backfillLimit.plus(Duration.ofDays(2))
                        }

                        val endDate = user.endDate
                        if (endDate <= startOffset) return@flatMap emptySequence()
                        val endTime = (startOffset + defaultQueryRange).coerceAtMost(endDate)
                        route.generateRequests(user, startOffset, endTime, max / routes.size)
                    }
                    .takeWhile { !shouldBackoff }
        } else emptySequence()
    }

    override fun requestSuccessful(request: RestRequest, response: Response) {
        logger.debug("Request successful: {}. Writing to offsets...", request.request)
//        offsetPersistenceFactory.add(
//                Path.of(request.user.versionedId), UserRouteOffset(
//                request.user.versionedId,
//                request.route.toString(),
//                request.endDate
//        )
//        )
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
                                "Please enable in garmin connect. User backing off for $USER_BACK_OFF_TIME..."
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
        val JSON_READER = ObjectMapper(JSON_FACTORY)
                .registerModule(JavaTimeModule())
                .reader()
    }
}