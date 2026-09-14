package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomDataRange
import org.radarcns.connector.dexcom.DexcomDataRangeMoment
import org.radarcns.connector.dexcom.DexcomDataRangeWindow
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
            calibrations = get("calibrations")?.takeIf { !it.isNull }?.toWindow()
            egvs = get("egvs")?.takeIf { !it.isNull }?.toWindow()
            events = get("events")?.takeIf { !it.isNull }?.toWindow()
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun JsonNode.toWindow(): DexcomDataRangeWindow =
        DexcomDataRangeWindow.newBuilder().apply {
            start = get("start")?.takeIf { !it.isNull }?.toMoment() ?: emptyMoment()
            end = get("end")?.takeIf { !it.isNull }?.toMoment() ?: emptyMoment()
        }.build()

    private fun JsonNode.toMoment(): DexcomDataRangeMoment =
        DexcomDataRangeMoment.newBuilder().apply {
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
}
