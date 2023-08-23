package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraHeartRateVariability
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.io.IOException
import org.radarbase.oura.user.User

class OuraSleepHrvConverter(
    private val topic: String = "connect_oura_heart_rate_variability",
) : OuraDataConverter {

    @Throws(IOException::class)
    override fun processRecords(
        root: JsonNode,
        user: User
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
        .flatMap { 
            val data = it.get("hrv")
            val interval = data?.get("interval")?.intValue() ?: throw IOException()
            val items = data.get("items")
            val startTime = OffsetDateTime.parse(data.get("timestamp").textValue())
            val startInstant = startTime.toInstant()
            if (items == null) emptySequence()
            else {
                items.asSequence()
                    .mapIndexedCatching { i, v -> 
                        TopicData(
                            key = user.observationKey,
                            topic = topic,
                            value = data.toHrv(startInstant, i, interval, v.floatValue()),
                        )
                    }
            }
        }
    }

    private fun JsonNode.toHrv(
        startTime: Instant,
        index: Int,
        interval: Int,
        value: Float
    ): OuraHeartRateVariability {
        val offset = interval * index
        return OuraHeartRateVariability.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0 + offset
            timeReceived = System.currentTimeMillis() / 1000.0
            hrv = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSleepHrvConverter::class.java)
    }
}
