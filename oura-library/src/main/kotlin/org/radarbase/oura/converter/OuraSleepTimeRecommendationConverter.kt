package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraRecommendedSleepTime
import org.radarcns.connector.oura.OuraSleepRecommendation
import org.radarcns.connector.oura.OuraSleepStatus
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class OuraSleepTimeRecommendationConverter(
    private val topic: String = "connect_oura_recommended_sleep_time",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val localDate = LocalDate.parse(
                    it["day"].textValue(),
                    DateTimeFormatter.ISO_LOCAL_DATE,
                )
                val startInstant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpoch(),
                    value = it.toSleepTime(startInstant),
                )
            }
    }

    private fun JsonNode.toSleepTime(
        startTime: Instant,
    ): OuraRecommendedSleepTime {
        val data = this
        return OuraRecommendedSleepTime.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id").textValue()
            day = data.get("day").textValue()
            optimalBedtimeStartOffset = data.get("optimal_bedtime")?.get("start_offset")?.intValue()
            optimalBedtimeEndOffset = data.get("optimal_bedtime")?.get("end_offset")?.intValue()
            optimalBedtimeTimezoneOffset = data.get("optimal_bedtime")?.get("day_tz")?.intValue()
            recommendation = data.get("recommendation").textValue()?.classifyRecommendation()
                ?: OuraSleepRecommendation.UNKNOWN
            status = data.get("status").textValue()?.classifyStatus()
                ?: OuraSleepStatus.UNKNOWN
        }.build()
    }

    private fun String.classifyRecommendation(): OuraSleepRecommendation {
        return when (this) {
            "improve_efficiency" -> OuraSleepRecommendation.IMPROVE_EFFICIENCY
            "earlier_bedtime" -> OuraSleepRecommendation.EARLIER_BEDTIME
            "later_bedtime" -> OuraSleepRecommendation.LATER_BEDTIME
            "earlier_wake_up_time" -> OuraSleepRecommendation.EARLIER_WAKE_UP_TIME
            "later_wake_up_time" -> OuraSleepRecommendation.LATER_WAKE_UP_TIME
            "follow_optimal_bedtime" -> OuraSleepRecommendation.FOLLOW_OPTIMAL_BEDTIME
            else -> OuraSleepRecommendation.UNKNOWN
        }
    }

    private fun String.classifyStatus(): OuraSleepStatus {
        return when (this) {
            "not_enough_nights" -> OuraSleepStatus.NOT_ENOUGH_NIGHTS
            "not_enough_recent_nights" -> OuraSleepStatus.NOT_ENOUGH_RECENT_NIGHTS
            "bad_sleep_quality" -> OuraSleepStatus.BAD_SLEEP_QUALITY
            "only_recommended_found" -> OuraSleepStatus.ONLY_RECOMMENDED_FOUND
            "optimal_found" -> OuraSleepStatus.OPTIMAL_FOUND
            else -> OuraSleepStatus.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSleepTimeRecommendationConverter::class.java)
    }
}
