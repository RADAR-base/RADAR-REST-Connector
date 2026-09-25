package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomDisplayDevice
import org.radarcns.connector.dexcom.DexcomEvent
import org.radarcns.connector.dexcom.DexcomEventStatus
import org.radarcns.connector.dexcom.DexcomEventSubType
import org.radarcns.connector.dexcom.DexcomEventType
import org.radarcns.connector.dexcom.DexcomTransmitterGeneration
import org.radarcns.connector.dexcom.DexcomTransmitterGenerationVariant
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
                val systemTimeInstant = DexcomEGVConverter.parseDexcomTime(
                    it.get("systemTime").asText(),
                )
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
            time = systemTimeInstant.epochSecond.toDouble()
            displayTime = textOrNull("displayTime")
            eventStatus = parseEventStatus(textOrNull("eventStatus"))
            eventType = parseEventType(textOrNull("eventType"))
            eventSubType = parseEventSubType(textOrNull("eventSubType"))
            value = valueAsStringOrNull("value")
            unit = textOrNull("unit")
            transmitterId = textOrNull("transmitterId").orEmpty()
            transmitterGeneration = parseTransmitterGeneration(textOrNull("transmitterGeneration"))
            transmitterGenerationVariant =
                parseTransmitterGenerationVariant(textOrNull("transmitterGenerationVariant"))
            displayDevice = parseDisplayDevice(textOrNull("displayDevice"))
            timeReceived = System.currentTimeMillis() / 1000.0
            recordedSystemTime = textOrNull("recordedSystemTime")
            recordedDisplayTime = textOrNull("recordedDisplayTime")
        }.build()

    private fun parseEventStatus(value: String?): DexcomEventStatus? =
        when (value) {
            null -> null
            else -> when (value.lowercase()) {
                "created" -> DexcomEventStatus.CREATED
                "updated" -> DexcomEventStatus.UPDATED
                "deleted" -> DexcomEventStatus.DELETED
                else -> DexcomEventStatus.UNKNOWN
            }
        }

    private fun parseEventType(value: String?): DexcomEventType? =
        when (value) {
            null -> null
            else -> when (value.lowercase()) {
                "insulin" -> DexcomEventType.INSULIN
                "carbs" -> DexcomEventType.CARBS
                "exercise" -> DexcomEventType.EXERCISE
                "health" -> DexcomEventType.HEALTH
                "bloodglucose" -> DexcomEventType.BLOOD_GLUCOSE
                "notes" -> DexcomEventType.NOTES
                else -> DexcomEventType.UNKNOWN
            }
        }

    private fun parseEventSubType(value: String?): DexcomEventSubType? =
        when (value) {
            null -> null
            else -> when (value.lowercase()) {
                "fastacting" -> DexcomEventSubType.FAST_ACTING
                "longacting" -> DexcomEventSubType.LONG_ACTING
                "light" -> DexcomEventSubType.LIGHT
                "medium" -> DexcomEventSubType.MEDIUM
                "heavy" -> DexcomEventSubType.HEAVY
                "illness" -> DexcomEventSubType.ILLNESS
                "stress" -> DexcomEventSubType.STRESS
                "highsymptoms" -> DexcomEventSubType.HIGH_SYMPTOMS
                "lowsymptoms" -> DexcomEventSubType.LOW_SYMPTOMS
                "cycle" -> DexcomEventSubType.CYCLE
                "alcohol" -> DexcomEventSubType.ALCOHOL
                else -> DexcomEventSubType.UNKNOWN
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

    /** Dexcom may return value as string or number; Avro field is string. */
    private fun JsonNode.valueAsStringOrNull(field: String): String? {
        val node = get(field)?.takeIf { !it.isNull } ?: return null
        return when {
            node.isNumber -> node.numberValue().toString()
            else -> node.asText()
        }
    }
}
