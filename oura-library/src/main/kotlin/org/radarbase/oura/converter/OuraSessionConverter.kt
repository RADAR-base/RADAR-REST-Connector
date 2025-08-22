package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraMomentMood
import org.radarcns.connector.oura.OuraMomentType
import org.radarcns.connector.oura.OuraSession
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraSessionConverter(
    private val topic: String = "connect_oura_session",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val startTime = OffsetDateTime.parse(it["start_datetime"].textValue())
                val startInstant = startTime.toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpoch(),
                    value = it.toSession(startInstant),
                )
            }
    }

    private fun JsonNode.toSession(
        startTime: Instant,
    ): OuraSession {
        val data = this
        return OuraSession.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            endTime = data.get("end_datetime")?.let {
                val endTime = OffsetDateTime.parse(it.textValue())
                endTime.toInstant().toEpochMilli() / 1000.0
            } ?: (startTime.toEpochMilli() / 1000.0)
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            type = data.get("type")?.textValue()?.classifyType()
                ?: OuraMomentType.UNKNOWN
            mood = data.get("mood")?.textValue()?.classifyMood()
                ?: OuraMomentMood.UNKNOWN
        }.build()
    }

    private fun String.classifyMood(): OuraMomentMood {
        return when (this) {
            "bad" -> OuraMomentMood.BAD
            "worse" -> OuraMomentMood.WORSE
            "same" -> OuraMomentMood.SAME
            "good" -> OuraMomentMood.GOOD
            "great" -> OuraMomentMood.GREAT
            else -> OuraMomentMood.UNKNOWN
        }
    }

    private fun String.classifyType(): OuraMomentType {
        return when (this) {
            "breathing" -> OuraMomentType.BREATHING
            "meditation" -> OuraMomentType.MEDITATION
            "nap" -> OuraMomentType.NAP
            "relaxation" -> OuraMomentType.RELAXATION
            "rest" -> OuraMomentType.REST
            "body_status" -> OuraMomentType.BODY_STATUS
            else -> OuraMomentType.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSessionConverter::class.java)
    }
}
