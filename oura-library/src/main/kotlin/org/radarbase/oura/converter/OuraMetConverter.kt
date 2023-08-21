package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraMet
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraMetConverter(
    private val topic: String = "connect_oura_met",
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
            val data = it.optObject("met")
            val interval = data?.optInt("interval")
            val items = data?.optArray("items")
            if (items == null) emptySequence()
            else {
                items.asSequence()
                    .mapIndexedCatching { i, v -> 
                        TopicData(
                            key = user.observationKey,
                            topic = topic,
                            value = data.toMet(startInstant, i, interval, v.floatValue()),
                        )
                    }
            }
        }
    }

    private fun JsonNode.toMet(
        startTime: Instant,
        index: Int?,
        interval: Int?,
        value: Float
    ): OuraMet {
        val offset = interval ?: 0 * index!!
        return OuraMet.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            met = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraMetConverter::class.java)
    }
}
