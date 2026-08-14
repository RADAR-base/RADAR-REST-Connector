package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomEvent
import java.time.Instant

class DexcomEventsConverter(
    private val topic: String = "connect_dexcom_event",
) : DexcomDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("records")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val systemTimeInstant = DexcomEGVConverter.parseDexcomTime(it.get("systemTime").asText())
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = systemTimeInstant.epochSecond,
                    value = it.toDexcomEvent(systemTimeInstant),
                )
            }
    }

    private fun JsonNode.toDexcomEvent(systemTimeInstant: Instant): DexcomEvent =
        DexcomEvent.newBuilder().apply {
            recordId = get("recordId").asText()
            systemTime = systemTimeInstant.epochSecond.toDouble()
            displayTime = textOrNull("displayTime")
            eventStatus = get("eventStatus").asText()
            eventType = get("eventType").asText()
            eventSubType = textOrNull("eventSubType")
            value = valueAsStringOrNull("value")
            unit = textOrNull("unit")
            transmitterId = textOrNull("transmitterId")
            transmitterGeneration = textOrNull("transmitterGeneration")
            transmitterGenerationVariant = textOrNull("transmitterGenerationVariant")
            displayDevice = textOrNull("displayDevice")
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun JsonNode.textOrNull(field: String): String? =
        get(field)?.takeIf { !it.isNull }?.asText()

    /** Dexcom may return value as string or number; Avro field is string. */
    private fun JsonNode.valueAsStringOrNull(field: String): String? {
        val node = get(field)?.takeIf { !it.isNull } ?: return null
        return when {
            node.isNumber -> node.numberValue().toString()
            else -> node.asText()
        }
    }
}
