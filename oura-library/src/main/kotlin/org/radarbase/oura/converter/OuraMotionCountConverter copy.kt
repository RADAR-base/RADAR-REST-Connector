package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraMotionCount
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraMotionCountConverter(
    private val topic: String = "connect_oura_motion_count",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
        .flatMap { 
            val data = it.get("motion_count")
            val interval = data?.get("interval")?.intValue()
            val items = data?.get("items")
            val startTime = OffsetDateTime.parse(data.get("timestamp").textValue())
            val startInstant = startTime.toInstant()
            if (items == null) emptySequence()
            else {
                items.asSequence()
                    .mapIndexedCatching { i, v -> 
                        TopicData(
                            key = user.observationKey,
                            topic = topic,
                            value = data.toMotionCount(startInstant, i, interval, v.intValue()),
                        )
                    }
            }
        }
    }

    private fun JsonNode.toMotionCount(
        startTime: Instant,
        index: Int?,
        interval: Int?,
        value: Int
    ): OuraMotionCount {
        val offset = interval ?: 0 * index!!
        return OuraMotionCount.newBuilder().apply {
            time = (startTime.toEpochMilli() / 1000.0) + offset
            timeReceived = System.currentTimeMillis() / 1000.0
            motionCount = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraMotionCountConverter::class.java)
    }
}
