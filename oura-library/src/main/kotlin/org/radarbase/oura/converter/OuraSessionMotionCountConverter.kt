package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraMotionCount
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime

class OuraSessionMotionCountConverter(
    private val topic: String = "connect_oura_motion_count",
    private val sampleKey: String = "motion_count",
) : OuraDataConverter {

    @Throws(IOException::class)
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .flatMap {
                runCatching {
                    it.processSamples(user)
                }.getOrElse {
                    logger.error("Error processing records", it.message)
                    emptySequence()
                }
            }
    }

    private fun JsonNode.processSamples(
        user: User,
    ): Sequence<Result<TopicData>> {
        val startTime = OffsetDateTime.parse(this["start_datetime"].textValue())
        val startTimeEpoch = startTime.toInstant().toEpochMilli() / 1000.0
        val timeReceivedEpoch = System.currentTimeMillis() / 1000.0
        val id = this.get("id").textValue()
        val interval = this.get(sampleKey)?.get("interval")?.intValue()
            ?: throw IOException("Unable to get sample interval.")
        val items = this.get(sampleKey)?.get("items") ?: throw IOException("Unable to get items.")
        return items.asSequence()
            .mapIndexedCatching { index, value ->
                val offset = interval * index
                val time = startTimeEpoch + offset
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = time.toLong(),
                    value = toMotionCount(
                        startTimeEpoch,
                        timeReceivedEpoch,
                        id,
                        value.intValue(),
                    ),
                )
            }
    }

    private fun toMotionCount(
        startTimeEpoch: Double,
        timeReceivedEpoch: Double,
        idString: String,
        value: Int,
    ): OuraMotionCount {
        return OuraMotionCount.newBuilder().apply {
            id = idString
            time = startTimeEpoch
            timeReceived = timeReceivedEpoch
            motionCount = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSessionMotionCountConverter::class.java)
    }
}
