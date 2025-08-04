package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraDailyReadiness
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraDailyReadinessConverter(
    private val topic: String = "connect_oura_daily_readiness",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val startTime = OffsetDateTime.parse(it["timestamp"].textValue())
                val startInstant = startTime.toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpoch(),
                    value = it.toDailyReadiness(startInstant),
                )
            }
    }

    private fun JsonNode.toDailyReadiness(
        startTime: Instant,
    ): OuraDailyReadiness {
        val data = this
        return OuraDailyReadiness.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            contributorActivityBalance =
                data.get("contributors")?.get("activity_balance")?.intValue()
            contributorBodyTemperature =
                data.get("contributors")?.get("body_temperature")?.intValue()
            contributorHrvBalance =
                data.get("contributors")?.get("hrv_balance")?.intValue()
            contributorPreviousDayActivity =
                data.get("contributors")?.get("previous_day_activity")?.intValue()
            contributorPreviousNight = data.get("contributors")?.get("previous_night")?.intValue()
            contributorRecoveryIndex = data.get("contributors")?.get("recovery_index")?.intValue()
            contributorRestingHeartRate =
                data.get("contributors")?.get("resting_heart_rate")?.intValue()
            contributorSleepBalance = data.get("contributors")?.get("sleep_balance")?.intValue()
            day = data.get("day")?.textValue()
            score = data.get("score")?.intValue()
            temperatureDeviation = data.get("temperature_deviation")?.floatValue()
            temperatureTrendDeviation = data.get("temperature_trend_deviation")?.floatValue()
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyReadinessConverter::class.java)
    }
}
