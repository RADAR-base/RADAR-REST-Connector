package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraHeartRate
import org.radarcns.connector.oura.OuraHeartRateSource
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraHeartRateConverter(
    private val topic: String = "connect_oura_heart_rate",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User
    ): Sequence<Result<TopicData>> {
        val array = root.optArray("data")
            ?: return emptySequence()
        return array.asSequence()
        .mapCatching { 
            val startTime = OffsetDateTime.parse(it["timestamp"].textValue())
            val startInstant = startTime.toInstant()
            TopicData(
                key = user.observationKey,
                topic = topic,
                value = it.toHeartRate(startInstant),
            )
        }
    }

    private fun JsonNode.toHeartRate(
        startTime: Instant,
    ): OuraHeartRate {
        return OuraHeartRate.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            source = optString("source")?.classify()
            bpm = optInt("bpm")
        }.build()
    }

    private fun String.classify() : OuraHeartRateSource {
        return when (this) {
            "awake" -> OuraHeartRateSource.AWAKE
            "rest" -> OuraHeartRateSource.REST
            "sleep" -> OuraHeartRateSource.SLEEP
            "session" -> OuraHeartRateSource.SESSION
            "live" -> OuraHeartRateSource.LIVE
            "workout" -> OuraHeartRateSource.WORKOUT
            else -> OuraHeartRateSource.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraHeartRateConverter::class.java)
    }
}
