package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomAlertSchedule
import org.radarcns.connector.dexcom.DexcomAlertScheduleOverride
import org.radarcns.connector.dexcom.DexcomAlertScheduleSettings
import org.radarcns.connector.dexcom.DexcomDevice
import org.radarcns.connector.dexcom.DexcomDeviceAlertSetting
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
            .mapCatching {
                val lastUpload = textOrNull(it, "lastUploadDate")
                val offsetInstant = lastUpload
                    ?.let { value -> runCatching { DexcomEGVConverter.parseDexcomTime(value) }.getOrNull() }
                    ?: Instant.now()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = offsetInstant.epochSecond,
                    value = it.toDexcomDevice(),
                )
            }
    }

    private fun JsonNode.toDexcomDevice(): DexcomDevice =
        DexcomDevice.newBuilder().apply {
            transmitterGeneration = textOrNull(this@toDexcomDevice, "transmitterGeneration")
            transmitterGenerationVariant = textOrNull(this@toDexcomDevice, "transmitterGenerationVariant")
            displayDevice = textOrNull(this@toDexcomDevice, "displayDevice")
            displayApp = textOrNull(this@toDexcomDevice, "displayApp")
            lastUploadDate = textOrNull(this@toDexcomDevice, "lastUploadDate")
            alertSchedules = get("alertSchedules")
                ?.takeIf { it.isArray }
                ?.map { it.toAlertSchedule() }
                ?: emptyList()
            transmitterId = textOrNull(this@toDexcomDevice, "transmitterId")
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun JsonNode.toAlertSchedule(): DexcomAlertSchedule =
        DexcomAlertSchedule.newBuilder().apply {
            alertScheduleSettings = get("alertScheduleSettings")?.toAlertScheduleSettings()
                ?: DexcomAlertScheduleSettings.newBuilder()
                    .setAlertScheduleName("")
                    .setIsEnabled(false)
                    .setStartTime("00:00")
                    .setEndTime("00:00")
                    .setDaysOfWeek(emptyList())
                    .build()
            alertSettings = get("alertSettings")
                ?.takeIf { it.isArray }
                ?.map { it.toDeviceAlertSetting() }
                ?: emptyList()
        }.build()

    private fun JsonNode.toAlertScheduleSettings(): DexcomAlertScheduleSettings {
        val builder = DexcomAlertScheduleSettings.newBuilder()
        builder.alertScheduleName = get("alertScheduleName")?.asText().orEmpty()
        builder.isEnabled = get("isEnabled")?.asBoolean() ?: false
        builder.startTime = get("startTime")?.asText().orEmpty()
        builder.endTime = get("endTime")?.asText().orEmpty()
        builder.isActive = booleanOrNull("isActive")
        builder.setOverride(get("override")?.takeIf { !it.isNull }?.toOverride())
        builder.daysOfWeek = get("daysOfWeek")
            ?.takeIf { it.isArray }
            ?.map { it.asText() }
            ?: emptyList()
        return builder.build()
    }

    private fun JsonNode.toOverride(): DexcomAlertScheduleOverride =
        DexcomAlertScheduleOverride.newBuilder().apply {
            isOverrideEnabled = booleanOrNull("isOverrideEnabled")
            mode = textOrNull(this@toOverride, "mode")
            endTime = textOrNull(this@toOverride, "endTime")
        }.build()

    private fun JsonNode.toDeviceAlertSetting(): DexcomDeviceAlertSetting =
        DexcomDeviceAlertSetting.newBuilder().apply {
            systemTime = get("systemTime")
                ?.takeIf { !it.isNull }
                ?.asText()
                ?.let { value ->
                    runCatching {
                        DexcomEGVConverter.parseDexcomTime(value).epochSecond.toDouble()
                    }.getOrNull()
                }
            displayTime = textOrNull(this@toDeviceAlertSetting, "displayTime")
            alertName = get("alertName").asText()
            value = intOrNull("value")
            unit = textOrNull(this@toDeviceAlertSetting, "unit")
            snooze = intOrNull("snooze")
            enabled = get("enabled")?.asBoolean() ?: false
            secondaryTriggerCondition = intOrNull("SecondaryTriggerCondition")
                ?: intOrNull("secondaryTriggerCondition")
            soundTheme = textOrNull(this@toDeviceAlertSetting, "soundTheme")
            soundOutputMode = textOrNull(this@toDeviceAlertSetting, "soundOutputMode")
        }.build()

    private fun textOrNull(node: JsonNode, field: String): String? =
        node.get(field)?.takeIf { !it.isNull }?.asText()

    private fun JsonNode.booleanOrNull(field: String): Boolean? =
        get(field)?.takeIf { !it.isNull }?.asBoolean()

    private fun JsonNode.intOrNull(field: String): Int? =
        get(field)?.takeIf { !it.isNull }?.asInt()
}
