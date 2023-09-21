package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraActivityClass
import org.radarcns.connector.oura.OuraActivityClassType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime

class OuraDailyActivityClassConverter(
    private val topic: String = "connect_oura_activity_class",
) : OuraDataConverter {

    final val ACTIVITY_CLASS_INTERVAL = 300 // in seconds

    @Throws(IOException::class)
    override fun processRecords(root: JsonNode, user: User): Sequence<Result<TopicData>> {
        val array = root.get("data") ?: return emptySequence()
        return array.asSequence().flatMap { it.processSamples(user) }
    }

    private fun JsonNode.processSamples(user: User): Sequence<Result<TopicData>> {
        val startTime = OffsetDateTime.parse(this["timestamp"].textValue())
        val startTimeEpoch = startTime.toInstant().toEpochMilli() / 1000.0
        val timeReceivedEpoch = System.currentTimeMillis() / 1000.0
        val id = this.get("id").textValue()
        val items = this.get("class_5_min").textValue().toCharArray()
        return if (items.isEmpty()) {
            emptySequence()
        } else {
            items.asSequence().mapIndexedCatching { index, value -> 
                val offset = ACTIVITY_CLASS_INTERVAL * index
                val time = startTimeEpoch + offset
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = time.toLong(),
                    value =
                    toActivityClass(
                        time,
                        timeReceivedEpoch,
                        id,
                        value.toString(),
                    ),
                )
            }
        }
    }

    private fun toActivityClass(
        startTimeEpoch: Double,
        timeReceivedEpoch: Double,
        idString: String,
        value: String,
    ): OuraActivityClass {
        return OuraActivityClass.newBuilder()
            .apply {
                id = idString
                time = startTimeEpoch
                timeReceived = timeReceivedEpoch
                type = value.classify()
            }
            .build()
    }

    private fun String.classify(): OuraActivityClassType {
        return when (this) {
            "0" -> OuraActivityClassType.NON_WEAR
            "1" -> OuraActivityClassType.REST
            "2" -> OuraActivityClassType.INACTIVE
            "3" -> OuraActivityClassType.LOW_ACTIVITY
            "4" -> OuraActivityClassType.MEDIUM_ACTIVITY
            "5" -> OuraActivityClassType.HIGH_ACTIVITY
            else -> OuraActivityClassType.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyActivityClassConverter::class.java)
    }
}
