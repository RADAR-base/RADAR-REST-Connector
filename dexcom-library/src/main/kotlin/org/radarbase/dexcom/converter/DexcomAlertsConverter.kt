package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomAlert
import org.radarcns.connector.dexcom.DexcomAlertName
import org.radarcns.connector.dexcom.DexcomAlertState
import org.radarcns.connector.dexcom.DexcomDisplayDevice
import org.radarcns.connector.dexcom.DexcomTransmitterGeneration
import org.radarcns.connector.dexcom.DexcomTransmitterGenerationVariant
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
                val systemTimeInstant = DexcomEGVConverter.parseDexcomTime(
                    it.get("systemTime").asText(),
                )
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
            time = systemTimeInstant.epochSecond.toDouble()
            displayTime = textOrNull("displayTime")
            alertName = parseAlertName(textOrNull("alertName"))
            alertState = parseAlertState(textOrNull("alertState"))
            displayDevice = parseDisplayDevice(textOrNull("displayDevice"))
            transmitterGeneration = parseTransmitterGeneration(textOrNull("transmitterGeneration"))
            transmitterGenerationVariant =
                parseTransmitterGenerationVariant(textOrNull("transmitterGenerationVariant"))
            transmitterId = textOrNull("transmitterId").orEmpty()
            displayApp = textOrNull("displayApp")
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun parseAlertName(value: String?): DexcomAlertName? =
        when (value) {
            null -> null
            else -> when (value.lowercase()) {
                "high" -> DexcomAlertName.HIGH
                "low" -> DexcomAlertName.LOW
                "rise" -> DexcomAlertName.RISE
                "fall" -> DexcomAlertName.FALL
                "outofrange" -> DexcomAlertName.OUT_OF_RANGE
                "urgentlow" -> DexcomAlertName.URGENT_LOW
                "urgentlowsoon" -> DexcomAlertName.URGENT_LOW_SOON
                "noreadings" -> DexcomAlertName.NO_READINGS
                else -> DexcomAlertName.UNKNOWN
            }
        }

    private fun parseAlertState(value: String?): DexcomAlertState? =
        when (value) {
            null -> null
            else -> when (value.lowercase()) {
                "inactive" -> DexcomAlertState.INACTIVE
                "activesnoozed" -> DexcomAlertState.ACTIVE_SNOOZED
                "activealarming" -> DexcomAlertState.ACTIVE_ALARMING
                else -> DexcomAlertState.UNKNOWN
            }
        }

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
}
