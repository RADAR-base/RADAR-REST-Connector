package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.radarcns.connector.oura.OuraDailySleep

class OuraDailySleepConverter(
        private val topic: String = "connect_oura_daily_sleep"
) : OuraDataConverter {
    override fun processRecords(
            root: JsonNode
    ): Sequence<Result<TopicData>> {
        val array = root.optArray("data")
                ?: return emptySequence()

        return array.asSequence()
                .sortedBy { it["timestamp"].textValue() }
                .mapCatching { s ->
                    val startTime = OffsetDateTime.parse(s["timestamp"].textValue())
                    val startInstant = startTime.toInstant()
                    TopicData(
                            sourceOffset = startInstant,
                            topic = topic,
                            value = s.toDailySleep(startInstant, startTime.offset),
                    )
                }
    }

    private fun JsonNode.toDailySleep(
            startTime: Instant,
            offset: ZoneOffset,
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
        private val logger = LoggerFactory.getLogger(OuraDailySleepConverter::class.java)
        private const val FOOD_CAL_TO_KJOULE_FACTOR = 4.1868f
    }
}