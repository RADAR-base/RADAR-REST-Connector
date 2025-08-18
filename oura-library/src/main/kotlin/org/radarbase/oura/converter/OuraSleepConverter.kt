package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraSleep
import org.radarcns.connector.oura.OuraSleepType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraSleepConverter(
    private val topic: String = "connect_oura_sleep",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val startTime = OffsetDateTime.parse(it["bedtime_start"].textValue())
                val startInstant = startTime.toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpoch(),
                    value = it.toSleep(startInstant),
                )
            }
    }

    private fun JsonNode.toSleep(
        startTime: Instant,
    ): OuraSleep {
        val data = this
        return OuraSleep.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            averageBreath = data.get("average_breath")?.floatValue()
            averageHeartRate = data.get("average_heart_rate")?.floatValue()
            averageHrv = data.get("average_hrv")?.intValue()
            awakeTime = data.get("awake_time")?.intValue()
            bedtimeEnd = data.get("bedtime_end")?.textValue()
            bedtimeStart = data.get("bedtime_start")?.textValue()
            day = data.get("day")?.textValue()
            deepSleepDuration = data.get("deep_sleep_duration")?.intValue()
            efficiency = data.get("efficiency")?.intValue()
            latency = data.get("latency")?.intValue()
            lightSleepDuration = data.get("light_sleep_duration")?.intValue()
            lowBatteryAlert = data.get("low_battery_alert")?.booleanValue()
            lowestHeartRate = data.get("lowest_heart_rate")?.intValue()
            period = data.get("period")?.intValue()
            readinessContributorActivityBalance =
                data.get("readiness")?.get("contributors")?.get("activity_balance")?.intValue()
            readinessContributorBodyTemperature =
                data.get("readiness")?.get("contributors")?.get("body_temperature")?.intValue()
            readinessContributorHrvBalance =
                data.get("readiness")?.get("contributors")?.get("hrv_balance")?.intValue()
            readinessContributorPreviousDayActivity =
                data.get("readiness")?.get("contributors")?.get("previous_day_activity")?.intValue()
            readinessContributorPreviousNight =
                data.get("readiness")?.get("contributors")?.get("previous_night")?.intValue()
            readinessContributorRecoveryIndex =
                data.get("readiness")?.get("contributors")?.get("recovery_index")?.intValue()
            readinessContributorRestingHeartRate =
                data.get("readiness")?.get("contributors")?.get("resting_heart_rate")?.intValue()
            readinessContributorSleepBalance =
                data.get("readiness")?.get("contributors")?.get("sleep_balance")?.intValue()
            readinessScore = data.get("readiness")?.get("score")?.intValue()
            readinessTemperatureDeviation =
                data.get("readiness")?.get("temperature_deviation")?.intValue()
            readinessTemperatureTrendDeviation =
                data.get("readiness")?.get("temperature_trend_deviation")?.intValue()
            readinessScoreDelta = data.get("readiness_score_delta")?.intValue()
            remSleepDuration = data.get("rem_sleep_duration")?.intValue()
            restlessPeriods = data.get("restless_periods")?.intValue()
            sleepScoreDelta = data.get("sleep_score_delta")?.intValue()
            timeInBed = data.get("time_in_bed")?.intValue()
            totalSleepDuration = data.get("total_sleep_duration")?.intValue()
            type = data.get("type")?.textValue()?.classifyType()
                ?: OuraSleepType.UNKNOWN
        }.build()
    }

    private fun String.classifyType(): OuraSleepType {
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
