package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomAlertName
import org.radarcns.connector.dexcom.DexcomDevice
import org.radarcns.connector.dexcom.DexcomDisplayDevice
import org.radarcns.connector.dexcom.DexcomTransmitterGeneration
import org.radarcns.connector.dexcom.DexcomTransmitterGenerationVariant
import java.time.Instant

class DexcomDevicesConverter(
    private val topic: String = "connect_dexcom_device",
) : DexcomDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("records")
            ?: return emptySequence()
        return array.asSequence()
            .flatMap { device ->
                val lastUpload = textOrNull(device, "lastUploadDate")
                val offsetInstant = lastUpload
                    ?.let { value -> runCatching { DexcomEGVConverter.parseDexcomTime(value) }.getOrNull() }
                    ?: Instant.now()
                val schedules = device.get("alertSchedules")?.takeIf { it.isArray }
                    ?: emptyList()
                schedules.asSequence().flatMap { schedule ->
                    val settings = schedule.get("alertScheduleSettings")
                    val alerts = schedule.get("alertSettings")?.takeIf { it.isArray }
                        ?: emptyList()
                    alerts.asSequence().map { alert ->
                        DeviceAlertRow(device, settings, alert, offsetInstant)
                    }
                }
            }
            .mapCatching { row ->
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = row.offset.epochSecond,
                    value = row.toDexcomDevice(),
                )
            }
    }

    private fun DeviceAlertRow.toDexcomDevice(): DexcomDevice {
        val override = settings?.get("override")?.takeIf { !it.isNull }
        val days = settings?.get("daysOfWeek")?.takeIf { it.isArray }
            ?.map { it.asText().lowercase() }
            ?.toSet()
        return DexcomDevice.newBuilder().apply {
            transmitterGeneration = parseTransmitterGeneration(
                textOrNull(device, "transmitterGeneration"),
            )
            transmitterGenerationVariant = parseTransmitterGenerationVariant(
                textOrNull(device, "transmitterGenerationVariant"),
            )
            displayDevice = parseDisplayDevice(textOrNull(device, "displayDevice"))
            displayApp = textOrNull(device, "displayApp")
            lastUploadDate = textOrNull(device, "lastUploadDate")
            transmitterId = textOrNull(device, "transmitterId")
            alertScheduleName = settings?.get("alertScheduleName")?.asText().orEmpty()
            isEnabled = settings?.get("isEnabled")?.asBoolean() ?: false
            startTime = settings?.get("startTime")?.asText().orEmpty()
            endTime = settings?.get("endTime")?.asText().orEmpty()
            isActive = settings?.booleanOrNull("isActive")
            isOverrideEnabled = override?.booleanOrNull("isOverrideEnabled")
            overrideMode = override?.let { textOrNull(it, "mode") }
            overrideEndTime = override?.let { textOrNull(it, "endTime") }
            appliesOnSunday = days?.contains("sunday")
            appliesOnMonday = days?.contains("monday")
            appliesOnTuesday = days?.contains("tuesday")
            appliesOnWednesday = days?.contains("wednesday")
            appliesOnThursday = days?.contains("thursday")
            appliesOnFriday = days?.contains("friday")
            appliesOnSaturday = days?.contains("saturday")
            systemTime = alert.get("systemTime")
                ?.takeIf { !it.isNull }
                ?.asText()
                ?.let { value ->
                    runCatching {
                        DexcomEGVConverter.parseDexcomTime(value).epochSecond.toDouble()
                    }.getOrNull()
                }
            displayTime = textOrNull(alert, "displayTime")
            alertName = parseAlertName(textOrNull(alert, "alertName"))
            value = alert.intOrNull("value")
            unit = textOrNull(alert, "unit")
            snooze = alert.intOrNull("snooze")
            enabled = alert.get("enabled")?.asBoolean() ?: false
            secondaryTriggerCondition = alert.intOrNull("SecondaryTriggerCondition")
                ?: alert.intOrNull("secondaryTriggerCondition")
            soundTheme = textOrNull(alert, "soundTheme")
            soundOutputMode = textOrNull(alert, "soundOutputMode")
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()
    }

    private fun parseAlertName(value: String?): DexcomAlertName? =
        when (value) {
            null -> null
            else -> when (value.lowercase().replace(" ", "").replace("_", "")) {
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

    private fun textOrNull(node: JsonNode, field: String): String? =
        node.get(field)?.takeIf { !it.isNull }?.asText()

    private fun JsonNode.booleanOrNull(field: String): Boolean? =
        get(field)?.takeIf { !it.isNull }?.asBoolean()

    private fun JsonNode.intOrNull(field: String): Int? =
        get(field)?.takeIf { !it.isNull }?.asInt()

    private data class DeviceAlertRow(
        val device: JsonNode,
        val settings: JsonNode?,
        val alert: JsonNode,
        val offset: Instant,
    )
}
