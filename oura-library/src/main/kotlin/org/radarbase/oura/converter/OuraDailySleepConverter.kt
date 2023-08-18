package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraDailySleep
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraDailySleepConverter(
    private val topic: String = "connect_oura_daily_sleep",
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
                value = it.toDailySleep(startInstant),
            )
        }
    }

    private fun JsonNode.toDailySleep(
        startTime: Instant,
    ): OuraDailySleep {
        return OuraDailySleep.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = optString("id")
            contributorDeepSleep = optObject("contributors")?.optInt("deep_sleep")
            contributorEfficiency = optObject("contributors")?.optInt("efficiency")
            contributorLatency = optObject("contributors")?.optInt("latency")
            contributorRemSleep = optObject("contributors")?.optInt("rem_sleep")
            contributorRestfulness = optObject("contributors")?.optInt("restfulness")
            contributorTiming = optObject("contributors")?.optInt("timing")
            contributorTotalSleep = optObject("contributors")?.optInt("total_sleep")
            day = optString("day")
            score = optInt("score")
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailySleepConverter::class.java)
    }
}
