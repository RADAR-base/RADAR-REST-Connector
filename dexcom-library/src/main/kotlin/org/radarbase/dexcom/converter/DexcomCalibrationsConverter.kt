package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomCalibration
import java.time.Instant

class DexcomCalibrationsConverter(
    private val topic: String = "connect_dexcom_calibration",
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
                    value = it.toDexcomCalibration(systemTimeInstant),
                )
            }
    }

    private fun JsonNode.toDexcomCalibration(systemTimeInstant: Instant): DexcomCalibration =
        DexcomCalibration.newBuilder().apply {
            recordId = get("recordId").asText()
            systemTime = systemTimeInstant.epochSecond.toDouble()
            displayTime = textOrNull("displayTime")
            unit = textOrNull("unit")
            value = intOrNull("value")
            displayDevice = textOrNull("displayDevice")
            transmitterId = textOrNull("transmitterId")
            transmitterTicks = longOrNull("transmitterTicks")
            transmitterGeneration = textOrNull("transmitterGeneration")
            transmitterGenerationVariant = textOrNull("transmitterGenerationVariant")
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun JsonNode.textOrNull(field: String): String? =
        get(field)?.takeIf { !it.isNull }?.asText()

    private fun JsonNode.intOrNull(field: String): Int? =
        get(field)?.takeIf { !it.isNull }?.asInt()

    private fun JsonNode.longOrNull(field: String): Long? =
        get(field)?.takeIf { !it.isNull }?.asLong()
}
