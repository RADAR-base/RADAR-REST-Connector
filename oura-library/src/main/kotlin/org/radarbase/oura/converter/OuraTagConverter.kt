package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraTag
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraTagConverter(
    private val topic: String = "connect_oura_tag",
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
            val tags = it.optArray("tags")
            val data = it
            if (tags == null) emptySequence()
            else {
                tags.asSequence()
                    .mapCatching {
                        TopicData(
                            key = user.observationKey,
                            topic = topic,
                            value = data.toTag(startInstant, it.textValue()),
                        )
                    }
            }
        }
    }

    private fun JsonNode.toTag(
        startTime: Instant,
        tagString: String
    ): OuraTag {
        return OuraTag.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = optString("id")
            day = optString("day")
            text = optString("text")
            tag = tagString
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraTagConverter::class.java)
    }
}
