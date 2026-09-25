package org.radarbase.dexcom.request

import com.fasterxml.jackson.databind.JsonNode
import okhttp3.OkHttpClient
import org.radarbase.dexcom.converter.DexcomEGVConverter
import org.radarbase.dexcom.route.DexcomAlertsRoute
import org.radarbase.dexcom.route.DexcomCalibrationsRoute
import org.radarbase.dexcom.route.DexcomDataRangeRoute
import org.radarbase.dexcom.route.DexcomEGVRoute
import org.radarbase.dexcom.route.DexcomEventsRoute
import org.radarbase.dexcom.route.Route
import org.radarbase.dexcom.user.User
import org.radarbase.dexcom.user.UserRepository
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Instant

data class DexcomTimeWindow(
    val start: Instant,
    val end: Instant,
)

/**
 * Fetches Dexcom `/dataRange` once per user and reuses it for dated first pulls.
 */
class DexcomDataRangeCache(
    userRepository: UserRepository,
    private val apiBaseUrl: String,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val dataRangeRoute = DexcomDataRangeRoute(userRepository, apiBaseUrl)
    private val cache = mutableMapOf<String, UserDataRange>()

    @Throws(IOException::class)
    fun windowFor(user: User, route: Route): DexcomTimeWindow? =
        rangesFor(user).windowFor(route)

    @Throws(IOException::class)
    private fun rangesFor(user: User): UserDataRange {
        cache[user.versionedId]?.let { return it }
        val fetched = fetch(user)
        if (fetched.hasAnyWindow()) {
            cache[user.versionedId] = fetched
        } else {
            logger.warn(
                "dataRange for {} has no parseable egv, event, or calibration window",
                user.versionedId,
            )
        }
        return fetched
    }

    @Throws(IOException::class)
    private fun fetch(user: User): UserDataRange {
        val request = dataRangeRoute.createRequest(
            user,
            "$apiBaseUrl/${dataRangeRoute.subPath()}",
            "",
        )
        logger.info("Fetching dataRange for {}", user.versionedId)
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("dataRange request failed (${response.code}): $body")
            }
            val root = DexcomRequestGenerator.JSON_READER.readTree(body)
            return UserDataRange(
                egvs = root.window("egvs"),
                events = root.window("events"),
                calibrations = root.window("calibrations"),
            )
        }
    }

    private data class UserDataRange(
        val egvs: DexcomTimeWindow?,
        val events: DexcomTimeWindow?,
        val calibrations: DexcomTimeWindow?,
    ) {
        fun hasAnyWindow(): Boolean =
            egvs != null || events != null || calibrations != null

        fun windowFor(route: Route): DexcomTimeWindow? =
            when (route) {
                is DexcomEGVRoute -> egvs
                is DexcomEventsRoute -> events
                is DexcomCalibrationsRoute -> calibrations
                is DexcomAlertsRoute -> union()
                else -> null
            }

        private fun union(): DexcomTimeWindow? {
            val windows = listOfNotNull(egvs, events, calibrations)
            if (windows.isEmpty()) {
                return null
            }
            return DexcomTimeWindow(
                start = windows.minOf { it.start },
                end = windows.maxOf { it.end },
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DexcomDataRangeCache::class.java)

        private fun JsonNode.window(field: String): DexcomTimeWindow? {
            val node = get(field)?.takeIf { !it.isNull } ?: return null
            val start = node.get("start").instantOrNull() ?: return null
            val end = node.get("end").instantOrNull() ?: return null
            if (start.isAfter(end)) {
                return null
            }
            return DexcomTimeWindow(start, end)
        }

        private fun JsonNode?.instantOrNull(): Instant? {
            val text = this?.get("systemTime")?.takeIf { !it.isNull }?.asText() ?: return null
            return runCatching { DexcomEGVConverter.parseDexcomTime(text) }.getOrNull()
        }
    }
}
