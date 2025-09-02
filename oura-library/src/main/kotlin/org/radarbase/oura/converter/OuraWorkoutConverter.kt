package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraWorkout
import org.radarcns.connector.oura.OuraWorkoutIntensity
import org.radarcns.connector.oura.OuraWorkoutSource
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraWorkoutConverter(
    private val topic: String = "connect_oura_workout",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val startTime = OffsetDateTime.parse(it["start_datetime"].textValue())
                val startInstant = startTime.toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpoch(),
                    value = it.toWorkout(startInstant),
                )
            }
    }

    private fun JsonNode.toWorkout(
        startTime: Instant,
    ): OuraWorkout {
        val data = this
        return OuraWorkout.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            endTime = data.get("end_datetime")?.let {
                val endTime = OffsetDateTime.parse(it.textValue())
                endTime.toInstant().toEpochMilli() / 1000.0
            } ?: (startTime.toEpochMilli() / 1000.0)
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            activity = data.get("activity")?.textValue()
            calories = data.get("calories")?.floatValue()
            day = data.get("day")?.textValue()
            distance = data.get("distance")?.floatValue()
            intensity = data.get("intensity")?.textValue()?.classifyIntensity()
                ?: OuraWorkoutIntensity.UNKNOWN
            label = data.get("label")?.textValue()
            source = data.get("source")?.textValue()?.classifySource()
                ?: OuraWorkoutSource.UNKNOWN
        }.build()
    }

    private fun String.classifySource(): OuraWorkoutSource {
        return when (this) {
            "manual" -> OuraWorkoutSource.MANUAL
            "autodetected" -> OuraWorkoutSource.AUTODETECTED
            "confirmed" -> OuraWorkoutSource.CONFIRMED
            "workout_heart_rate" -> OuraWorkoutSource.WORKOUT_HEART_RATE
            else -> OuraWorkoutSource.UNKNOWN
        }
    }

    private fun String.classifyIntensity(): OuraWorkoutIntensity {
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
