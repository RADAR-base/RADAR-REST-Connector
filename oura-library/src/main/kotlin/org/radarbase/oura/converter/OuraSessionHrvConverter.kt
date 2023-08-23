package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraHeartRateVariability
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraSessionHrvConverter(
    private val topic: String = "connect_oura_heart_rate_variability",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
        .flatMap { 
            val startTime = OffsetDateTime.parse(it["timestamp"].textValue())
            val startInstant = startTime.toInstant()
            val data = it.get("heart_rate_variability")
            val interval = data?.get("interval")?.intValue()
            val items = data?.get("items")
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
        index: Int?,
        interval: Int?,
        value: Float
    ): OuraHeartRateVariability {
        val offset = interval ?: 0 * index!!
        return OuraHeartRateVariability.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0 + offset
            timeReceived = System.currentTimeMillis() / 1000.0
            hrv = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSessionHrvConverter::class.java)
    }
}
