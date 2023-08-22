package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraDailyActivity
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraDailyActivityConverter(
    private val topic: String = "connect_oura_daily_activity",
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
                value = it.toDailyActivity(startInstant),
            )
        }
    }

    private fun JsonNode.toDailyActivity(
        startTime: Instant,
    ): OuraDailyActivity {
        return OuraDailyActivity.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = optString("id")
            activeCalories = optInt("active_calories")
            contributorMeetDailyTargets = optObject("contributors")?.optInt("meet_daily_targets")
            contributorMoveEveryHour = optObject("contributors")?.optInt("move_every_hour")
            contributorRecoveryTime = optObject("contributors")?.optInt("recovery_time")
            contributorStayActive = optObject("contributors")?.optInt("stay_active")
            contributorTrainingFrequency = optObject("contributors")?.optInt("training_frequency")
            contributorTrainingVolume = optObject("contributors")?.optInt("training_volume")
            equivalentWalkingDistance = optInt("equivalent_walking_distance")
            highActivityMetMinutes = optInt("high_activity_met_minutes")
            highActivityTime = optInt("high_activity_time")
            inactivityAlerts = optInt("inactivity_alerts")
            lowActivityMetMinutes = optInt("low_activity_met_minutes")
            lowActivityTime = optInt("low_activity_time")
            mediumActivityMetMinutes = optInt("medium_activity_met_minutes")
            mediumActivityTime = optInt("medium_activity_time")
            metersToTarget  = optInt("meters_to_target")
            nonWearTime = optInt("non_wear_time")
            restingTime = optInt("resting_time")
            sedentaryMetMinutes = optInt("sedentary_met_minutes")
            sedentaryTime = optInt("sedentary_time")
            steps = optInt("steps")
            targetCalories = optInt("target_calories")
            targetMeters = optInt("target_meters")
            totalCalories = optInt("total_calories")
            day = optString("day")
            score = optInt("score")
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyActivityConverter::class.java)
    }
}
