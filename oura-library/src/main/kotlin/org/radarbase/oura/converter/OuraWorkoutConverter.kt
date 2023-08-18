package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraWorkout
import org.radarcns.connector.oura.OuraWorkoutSource
import org.radarcns.connector.oura.OuraWorkoutIntensity
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraWorkoutConverter(
    private val topic: String = "connect_oura_workout",
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
                value = it.toWorkout(startInstant),
            )
        }
    }

    private fun JsonNode.toWorkout(
        startTime: Instant,
    ): OuraWorkout {
        return OuraWorkout.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            endTime = OffsetDateTime.parse(optString("end_datetime")).toInstant().toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = optString("id")
            activity = optString("activity")
            calories = optFloat("calories")
            day = optString("day")
            distance = optFloat("distance")
            intensity = optString("intensity")?.classifyIntensity()
            label = optString("label")
            source = optString("source")?.classifySource()
        }.build()
    }

    private fun String.classifySource() : OuraWorkoutSource {
        return when (this) {
            "manual" -> OuraWorkoutSource.MANUAL
            "autodetected" -> OuraWorkoutSource.AUTODETECTED
            "confirmed" -> OuraWorkoutSource.CONFIRMED
            "workout_heart_rate" -> OuraWorkoutSource.WORKOUT_HEART_RATE
            else -> OuraWorkoutSource.UNKNOWN
        }
    }

    private fun String.classifyIntensity() : OuraWorkoutIntensity {
        return when (this) {
            "easy" -> OuraWorkoutIntensity.EASY
            "moderate" -> OuraWorkoutIntensity.MODERATE
            "hard" -> OuraWorkoutIntensity.HARD
            else -> OuraWorkoutIntensity.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraWorkoutConverter::class.java)
    }
}
