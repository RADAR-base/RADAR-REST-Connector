package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomAlert
import java.time.Instant

class DexcomAlertsConverter(
    private val topic: String = "connect_dexcom_alert",
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
                    value = it.toDexcomAlert(systemTimeInstant),
                )
            }
    }

    private fun JsonNode.toDexcomAlert(systemTimeInstant: Instant): DexcomAlert =
        DexcomAlert.newBuilder().apply {
            recordId = get("recordId").asText()
            systemTime = systemTimeInstant.epochSecond.toDouble()
            displayTime = textOrNull("displayTime")
            alertName = get("alertName").asText()
            alertState = get("alertState").asText()
            displayDevice = textOrNull("displayDevice")
            transmitterGeneration = textOrNull("transmitterGeneration")
            transmitterGenerationVariant = textOrNull("transmitterGenerationVariant")
            transmitterId = textOrNull("transmitterId")
            displayApp = textOrNull("displayApp")
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun JsonNode.textOrNull(field: String): String? =
        get(field)?.takeIf { !it.isNull }?.asText()
}
