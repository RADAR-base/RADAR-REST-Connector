package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraSleep
import org.radarcns.connector.oura.OuraSleepType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraSleepConverter(
    private val topic: String = "connect_oura_sleep",
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
                value = it.toSleep(startInstant),
            )
        }
    }

    private fun JsonNode.toSleep(
        startTime: Instant,
    ): OuraSleep {
        return OuraSleep.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = optString("id")
            averageBreath = optFloat("average_breath")
            averageHeartRate = optFloat("average_heart_rate")
            averageHrv = optInt("average_hrv")
            awakeTime = optInt("awake_time")
            bedtimeEnd = optString("bedtime_end")
            bedtimeStart = optString("bedtime_start")
            day = optString("day")
            deepSleepDuration = optInt("deep_sleep_duration")
            efficiency = optInt("efficiency")
            latency = optInt("latency")
            lightSleepDuration = optInt("light_sleep_duration")
            lowBatteryAlert = optBoolean("low_battery_alert")
            lowestHeartRate = optInt("lowest_heart_rate")
            period = optInt("period")
            readinessContributorActivityBalance = optObject("readiness")?.optObject("contributors")?.optInt("activity_balance")
            readinessContributorBodyTemperature = optObject("readiness")?.optObject("contributors")?.optInt("body_temperature")
            readinessContributorHrvBalance = optObject("readiness")?.optObject("contributors")?.optInt("hrv_balance")
            readinessContributorPreviousDayActivity = optObject("readiness")?.optObject("contributors")?.optInt("previous_day_activity")
            readinessContributorPreviousNight = optObject("readiness")?.optObject("contributors")?.optInt("previous_night")
            readinessContributorRecoveryIndex = optObject("readiness")?.optObject("contributors")?.optInt("recovery_index")
            readinessContributorRestingHeartRate = optObject("readiness")?.optObject("contributors")?.optInt("resting_heart_rate")
            readinessContributorSleepBalance = optObject("readiness")?.optObject("contributors")?.optInt("sleep_balance")
            readinessScore = optObject("readiness")?.optInt("score")
            readinessTemperatureDeviation = optObject("readiness")?.optInt("temperature_deviation")
            readinessTemperatureTrendDeviation = optObject("readiness")?.optInt("temperature_trend_deviation")
            readinessScoreDelta = optInt("readiness_score_delta")
            remSleepDuration = optInt("rem_sleep_duration")
            restlessPeriods = optInt("restless_periods")
            sleepScoreDelta = optInt("sleep_score_delta")
            timeInBed = optInt("time_in_bed")
            totalSleepDuration = optInt("total_sleep_duration")
            type = optString("type")?.classifyType()
        }.build()
    }

    private fun String.classifyType() : OuraSleepType {
        return when (this) {
            "deleted" -> OuraSleepType.DELETED
            "sleep" -> OuraSleepType.SLEEP
            "long_sleep" -> OuraSleepType.LONG_SLEEP
            "late_nap" -> OuraSleepType.LATE_NAP
            "rest" -> OuraSleepType.REST
            else -> OuraSleepType.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSleepConverter::class.java)
    }
}
