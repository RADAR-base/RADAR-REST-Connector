package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraDailySleep
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraDailySleepConverter(
    private val topic: String = "connect_oura_daily_sleep",
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
                    value = it.toDailySleep(startInstant),
                )
            }
    }

    private fun JsonNode.toDailySleep(
        startTime: Instant,
    ): OuraDailySleep {
        val data = this
        return OuraDailySleep.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            contributorDeepSleep = data.get("contributors")?.get("deep_sleep")?.intValue()
            contributorEfficiency = data.get("contributors")?.get("efficiency")?.intValue()
            contributorLatency = data.get("contributors")?.get("latency")?.intValue()
            contributorRemSleep = data.get("contributors")?.get("rem_sleep")?.intValue()
            contributorRestfulness = data.get("contributors")?.get("restfulness")?.intValue()
            contributorTiming = data.get("contributors")?.get("timing")?.intValue()
            contributorTotalSleep = data.get("contributors")?.get("total_sleep")?.intValue()
            day = data.get("day")?.textValue()
            score = data.get("score")?.intValue()
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailySleepConverter::class.java)
    }
}
