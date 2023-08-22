package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraHeartRate
import org.radarcns.connector.oura.OuraHeartRateSource
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraSessionHeartRateConverter(
    private val topic: String = "connect_oura_heart_rate",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User
    ): Sequence<Result<TopicData>> {
        val array = root.optArray("data")
            ?: return emptySequence()
        return array.asSequence()
        .flatMap { 
            val startTime = OffsetDateTime.parse(it["timestamp"].textValue())
            val startInstant = startTime.toInstant()
            val data = it.optObject("heart_rate")
            val interval = data?.optInt("interval")
            val items = data?.optArray("items")
            if (items == null) emptySequence()
            else {
                items.asSequence()
                    .mapIndexedCatching { i, v -> 
                        TopicData(
                            key = user.observationKey,
                            topic = topic,
                            value = data.toHeartRate(startInstant, i, interval, v.intValue()),
                        )
                    }
            }
        }
    }

    private fun JsonNode.toHeartRate(
        startTime: Instant,
        index: Int?,
        interval: Int?,
        value: Int
    ): OuraHeartRate {
        val offset = interval ?: 0 * index!!
        return OuraHeartRate.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0 + offset
            timeReceived = System.currentTimeMillis() / 1000.0
            source = OuraHeartRateSource.SLEEP
            bpm = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSessionHeartRateConverter::class.java)
    }
}
