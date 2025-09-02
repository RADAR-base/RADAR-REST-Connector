package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraEnhancedTag
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraEnhancedTagConverter(
    private val topic: String = "connect_oura_enhanced_tag",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val startTime = OffsetDateTime.parse(it["start_time"].textValue())
                val startInstant = startTime.toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpoch(),
                    value = it.toEnhancedTag(startInstant),
                )
            }
    }

    private fun JsonNode.toEnhancedTag(
        startTime: Instant,
    ): OuraEnhancedTag {
        val data = this
        return OuraEnhancedTag.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            tagTypeCode = data.get("tag_type_code")?.textValue()
            tagStartTime = data.get("start_time")?.textValue()?.let {
                OffsetDateTime.parse(it).toInstant().toEpochMilli() / 1000.0
            }
            tagEndTime = data.get("end_time")?.textValue()?.let {
                OffsetDateTime.parse(it).toInstant().toEpochMilli() / 1000.0
            }
            startDay = data.get("start_day")?.textValue()
            endDay = data.get("end_day")?.textValue()
            comment = data.get("comment")?.textValue()
            customName = data.get("custom_name")?.textValue()
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraEnhancedTagConverter::class.java)
    }
}
