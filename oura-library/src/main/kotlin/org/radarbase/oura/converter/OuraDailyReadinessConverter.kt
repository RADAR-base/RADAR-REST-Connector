package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraDailyReadiness
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraDailyReadinessConverter(
    private val topic: String = "connect_oura_daily_readiness",
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
                value = it.toDailyReadiness(startInstant),
            )
        }
    }

    private fun JsonNode.toDailyReadiness(
        startTime: Instant,
    ): OuraDailyReadiness {
        return OuraDailyReadiness.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = optString("id")
            contributorActivityBalance = optObject("contributors")?.optInt("activity_balance")
            contributorBodyTemperature = optObject("contributors")?.optInt("body_temperature")
            contributorHrvBalance = optObject("contributors")?.optInt("hrv_balance")
            contributorPreviousDayActivity = optObject("contributors")?.optInt("previous_day_activity")
            contributorPreviousNight = optObject("contributors")?.optInt("previous_night")
            contributorRecoveryIndex = optObject("contributors")?.optInt("recovery_index")
            contributorRestingHeartRate = optObject("contributors")?.optInt("resting_heart_rate")
            contributorSleepBalance = optObject("contributors")?.optInt("sleep_balance")
            day = optString("day")
            score = optInt("score")
            temperatureDeviation = optFloat("temperature_deviation")
            temperatureTrendDeviation = optFloat("temperature_trend_deviation")
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyReadinessConverter::class.java)
    }
}
