package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraDailyActivity
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraDailyActivityConverter(
    private val topic: String = "connect_oura_daily_activity",
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
                    value = it.toDailyActivity(startInstant),
                )
            }
    }

    private fun JsonNode.toDailyActivity(
        startTime: Instant,
    ): OuraDailyActivity {
        val data = this
        return OuraDailyActivity.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            activeCalories = data.get("active_calories")?.intValue()
            contributorMeetDailyTargets =
                data.get("contributors")?.get("meet_daily_targets")?.intValue()
            contributorMoveEveryHour = data.get("contributors")?.get("move_every_hour")?.intValue()
            contributorRecoveryTime = data.get("contributors")?.get("recovery_time")?.intValue()
            contributorStayActive = data.get("contributors")?.get("stay_active")?.intValue()
            contributorTrainingFrequency =
                data.get("contributors")?.get("training_frequency")?.intValue()
            contributorTrainingVolume = data.get("contributors")?.get("training_volume")?.intValue()
            equivalentWalkingDistance = data.get("equivalent_walking_distance")?.intValue()
            highActivityMetMinutes = data.get("high_activity_met_minutes")?.intValue()
            highActivityTime = data.get("high_activity_time")?.intValue()
            inactivityAlerts = data.get("inactivity_alerts")?.intValue()
            lowActivityMetMinutes = data.get("low_activity_met_minutes")?.intValue()
            lowActivityTime = data.get("low_activity_time")?.intValue()
            mediumActivityMetMinutes = data.get("medium_activity_met_minutes")?.intValue()
            mediumActivityTime = data.get("medium_activity_time")?.intValue()
            metersToTarget = data.get("meters_to_target")?.intValue()
            nonWearTime = data.get("non_wear_time")?.intValue()
            restingTime = data.get("resting_time")?.intValue()
            sedentaryMetMinutes = data.get("sedentary_met_minutes")?.intValue()
            sedentaryTime = data.get("sedentary_time")?.intValue()
            steps = data.get("steps")?.intValue()
            targetCalories = data.get("target_calories").intValue()
            targetMeters = data.get("target_meters").intValue()
            totalCalories = data.get("total_calories").intValue()
            day = data.get("day").textValue()
            score = data.get("score").intValue()
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyActivityConverter::class.java)
    }
}
