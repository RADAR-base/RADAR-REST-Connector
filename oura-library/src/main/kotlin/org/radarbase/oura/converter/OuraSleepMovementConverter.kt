package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraSleepMovement
import org.radarcns.connector.oura.OuraSleepMovementType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime

class OuraSleepMovementConverter(
    private val topic: String = "connect_oura_sleep_movement",
) : OuraDataConverter {

    final val SLEEP_MOVEMENT_INTERVAL = 30 // in seconds

    @Throws(IOException::class)
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .flatMap {
                it.processSamples(user)
            }
    }

    private fun JsonNode.processSamples(
        user: User,
    ): Sequence<Result<TopicData>> {
        val startTime = OffsetDateTime.parse(this["bedtime_start"].textValue())
        val startTimeEpoch = startTime.toInstant().toEpochMilli() / 1000.0
        val timeReceivedEpoch = System.currentTimeMillis() / 1000.0
        val id = this.get("id").textValue()
        val items = this.get("movement_30_sec")?.textValue()?.toCharArray()
        return if (items == null || items.isEmpty()) {
            emptySequence()
        } else {
            items.asSequence()
                .mapIndexedCatching { index, value ->
                    val offset = SLEEP_MOVEMENT_INTERVAL * index
                    val time = startTimeEpoch + offset
                    TopicData(
                        key = user.observationKey,
                        topic = topic,
                        offset = time.toLong(),
                        value = toSleepMovement(
                            time,
                            timeReceivedEpoch,
                            id,
                            value.toString(),
                        ),
                    )
                }
        }
    }

    private fun toSleepMovement(
        startTimeEpoch: Double,
        timeReceivedEpoch: Double,
        idString: String,
        value: String,
    ): OuraSleepMovement {
        return OuraSleepMovement.newBuilder().apply {
            id = idString
            time = startTimeEpoch
            timeReceived = timeReceivedEpoch
            movement = value.classify()
        }.build()
    }

    private fun String.classify(): OuraSleepMovementType {
        return when (this) {
            "1" -> OuraSleepMovementType.NO_MOTION
            "2" -> OuraSleepMovementType.RESTLESS
            "3" -> OuraSleepMovementType.TOSSING_AND_TURNING
            "4" -> OuraSleepMovementType.ACTIVE
            else -> OuraSleepMovementType.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSleepMovementConverter::class.java)
    }
}
