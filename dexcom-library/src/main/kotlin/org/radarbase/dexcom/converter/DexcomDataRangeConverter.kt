package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomDataRange
import org.radarcns.connector.dexcom.DexcomDataRangeMoment
import org.radarcns.connector.dexcom.DexcomDataRangeMomentEnd
import org.radarcns.connector.dexcom.DexcomDataRangeMomentEgvsEnd
import org.radarcns.connector.dexcom.DexcomDataRangeMomentEgvsStart
import org.radarcns.connector.dexcom.DexcomDataRangeMomentEventsEnd
import org.radarcns.connector.dexcom.DexcomDataRangeMomentEventsStart
import org.radarcns.connector.dexcom.DexcomDataRangeWindow
import org.radarcns.connector.dexcom.DexcomDataRangeWindowEgvs
import org.radarcns.connector.dexcom.DexcomDataRangeWindowEvents
import java.time.Instant

/**
 * /dataRange returns a single object (not records[]).
 */
class DexcomDataRangeConverter(
    private val topic: String = "connect_dexcom_data_range",
) : DexcomDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        return sequenceOf(root).mapCatching {
            val value = it.toDexcomDataRange()
            val offset = it.offsetFromResponse()
            TopicData(
                key = user.observationKey,
                topic = topic,
                offset = offset,
                value = value,
            )
        }
    }

    private fun JsonNode.offsetFromResponse(): Long {
        val candidates = listOfNotNull(
            get("egvs")?.get("end")?.get("systemTime")?.asText(),
            get("events")?.get("end")?.get("systemTime")?.asText(),
            get("calibrations")?.get("end")?.get("systemTime")?.asText(),
        )
        return candidates.firstNotNullOfOrNull { text ->
            runCatching { DexcomEGVConverter.parseDexcomTime(text).epochSecond }.getOrNull()
        } ?: Instant.now().epochSecond
    }

    private fun JsonNode.toDexcomDataRange(): DexcomDataRange =
        DexcomDataRange.newBuilder().apply {
            calibrations = get("calibrations")?.takeIf { !it.isNull }?.toCalibrationsWindow()
            egvs = get("egvs")?.takeIf { !it.isNull }?.toEgvsWindow()
            events = get("events")?.takeIf { !it.isNull }?.toEventsWindow()
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun JsonNode.toCalibrationsWindow(): DexcomDataRangeWindow =
        DexcomDataRangeWindow.newBuilder().apply {
            start = get("start")?.toMoment() ?: emptyMoment()
            end = get("end")?.toMomentEnd() ?: emptyMomentEnd()
        }.build()

    private fun JsonNode.toEgvsWindow(): DexcomDataRangeWindowEgvs =
        DexcomDataRangeWindowEgvs.newBuilder().apply {
            start = get("start")?.toEgvsStart() ?: emptyEgvsStart()
            end = get("end")?.toEgvsEnd() ?: emptyEgvsEnd()
        }.build()

    private fun JsonNode.toEventsWindow(): DexcomDataRangeWindowEvents =
        DexcomDataRangeWindowEvents.newBuilder().apply {
            start = get("start")?.toEventsStart() ?: emptyEventsStart()
            end = get("end")?.toEventsEnd() ?: emptyEventsEnd()
        }.build()

    private fun JsonNode.toMoment(): DexcomDataRangeMoment =
        DexcomDataRangeMoment.newBuilder().apply {
            systemTime = systemTimeEpochOrNull()
            displayTime = textOrNull("displayTime")
        }.build()

    private fun JsonNode.toMomentEnd(): DexcomDataRangeMomentEnd =
        DexcomDataRangeMomentEnd.newBuilder().apply {
            systemTime = systemTimeEpochOrNull()
            displayTime = textOrNull("displayTime")
        }.build()

    private fun JsonNode.toEgvsStart(): DexcomDataRangeMomentEgvsStart =
        DexcomDataRangeMomentEgvsStart.newBuilder().apply {
            systemTime = systemTimeEpochOrNull()
            displayTime = textOrNull("displayTime")
        }.build()

    private fun JsonNode.toEgvsEnd(): DexcomDataRangeMomentEgvsEnd =
        DexcomDataRangeMomentEgvsEnd.newBuilder().apply {
            systemTime = systemTimeEpochOrNull()
            displayTime = textOrNull("displayTime")
        }.build()

    private fun JsonNode.toEventsStart(): DexcomDataRangeMomentEventsStart =
        DexcomDataRangeMomentEventsStart.newBuilder().apply {
            systemTime = systemTimeEpochOrNull()
            displayTime = textOrNull("displayTime")
        }.build()

    private fun JsonNode.toEventsEnd(): DexcomDataRangeMomentEventsEnd =
        DexcomDataRangeMomentEventsEnd.newBuilder().apply {
            systemTime = systemTimeEpochOrNull()
            displayTime = textOrNull("displayTime")
        }.build()

    private fun JsonNode.systemTimeEpochOrNull(): Double? =
        get("systemTime")
            ?.takeIf { !it.isNull }
            ?.asText()
            ?.let { text ->
                runCatching {
                    DexcomEGVConverter.parseDexcomTime(text).epochSecond.toDouble()
                }.getOrNull()
            }

    private fun JsonNode.textOrNull(field: String): String? =
        get(field)?.takeIf { !it.isNull }?.asText()

    private fun emptyMoment() =
        DexcomDataRangeMoment.newBuilder().build()

    private fun emptyMomentEnd() =
        DexcomDataRangeMomentEnd.newBuilder().build()

    private fun emptyEgvsStart() =
        DexcomDataRangeMomentEgvsStart.newBuilder().build()

    private fun emptyEgvsEnd() =
        DexcomDataRangeMomentEgvsEnd.newBuilder().build()

    private fun emptyEventsStart() =
        DexcomDataRangeMomentEventsStart.newBuilder().build()

    private fun emptyEventsEnd() =
        DexcomDataRangeMomentEventsEnd.newBuilder().build()
}
