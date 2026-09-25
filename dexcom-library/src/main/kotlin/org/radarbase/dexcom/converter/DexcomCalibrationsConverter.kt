package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomCalibration
import org.radarcns.connector.dexcom.DexcomDisplayDevice
import org.radarcns.connector.dexcom.DexcomTransmitterGeneration
import org.radarcns.connector.dexcom.DexcomTransmitterGenerationVariant
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
                val systemTimeInstant = DexcomEGVConverter.parseDexcomTime(
                    it.get("systemTime").asText(),
                )
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
            time = systemTimeInstant.epochSecond.toDouble()
            displayTime = textOrNull("displayTime")
            unit = textOrNull("unit")
            value = intOrNull("value")
            displayDevice = parseDisplayDevice(textOrNull("displayDevice"))
            transmitterId = textOrNull("transmitterId").orEmpty()
            transmitterTicks = longOrNull("transmitterTicks")
            transmitterGeneration = parseTransmitterGeneration(textOrNull("transmitterGeneration"))
            transmitterGenerationVariant =
                parseTransmitterGenerationVariant(textOrNull("transmitterGenerationVariant"))
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun parseTransmitterGeneration(value: String?): DexcomTransmitterGeneration =
        when (value?.lowercase()) {
            "g6" -> DexcomTransmitterGeneration.G6
            "g6+" -> DexcomTransmitterGeneration.G6_PLUS
            "g6pro" -> DexcomTransmitterGeneration.G6_PRO
            "g7" -> DexcomTransmitterGeneration.G7
            else -> DexcomTransmitterGeneration.UNKNOWN
        }

    private fun parseTransmitterGenerationVariant(value: String?): DexcomTransmitterGenerationVariant =
        when (value?.lowercase()) {
            "d1+" -> DexcomTransmitterGenerationVariant.D1_PLUS
            "g6" -> DexcomTransmitterGenerationVariant.G6
            "g7" -> DexcomTransmitterGenerationVariant.G7
            "g715day" -> DexcomTransmitterGenerationVariant.G7_15_DAY
            else -> DexcomTransmitterGenerationVariant.UNKNOWN
        }

    private fun parseDisplayDevice(value: String?): DexcomDisplayDevice? =
        when (value?.lowercase()) {
            null -> null
            "receiver" -> DexcomDisplayDevice.RECEIVER
            "ios" -> DexcomDisplayDevice.IOS
            "android" -> DexcomDisplayDevice.ANDROID
            else -> DexcomDisplayDevice.UNKNOWN
        }

    private fun JsonNode.textOrNull(field: String): String? =
        get(field)?.takeIf { !it.isNull }?.asText()

    private fun JsonNode.intOrNull(field: String): Int? =
        get(field)?.takeIf { !it.isNull }?.asInt()

    private fun JsonNode.longOrNull(field: String): Long? =
        get(field)?.takeIf { !it.isNull }?.asLong()
}
