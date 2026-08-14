package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomEgv
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class DexcomEGVConverter(
    private val topic: String = "connect_dexcom_egv",
) : DexcomDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("records")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val systemTimeInstant = parseDexcomTime(it.get("systemTime").asText())
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = systemTimeInstant.epochSecond,
                    value = it.toDexcomEgv(systemTimeInstant),
                )
            }
    }

    private fun JsonNode.toDexcomEgv(systemTimeInstant: Instant): DexcomEgv =
        DexcomEgv.newBuilder().apply {
            recordId = get("recordId").asText()
            systemTime = systemTimeInstant.epochSecond.toDouble()
            displayTime = textOrNull("displayTime")
            transmitterId = textOrNull("transmitterId")
            transmitterTicks = longOrNull("transmitterTicks")
            value = intOrNull("value")
            status = textOrNull("status")
            trend = textOrNull("trend")
            trendRate = doubleOrNull("trendRate")
            unit = get("unit")?.takeIf { !it.isNull }?.asText() ?: "unknown"
            rateUnit = textOrNull("rateUnit")
            displayDevice = textOrNull("displayDevice")
            transmitterGeneration = textOrNull("transmitterGeneration")
            transmitterGenerationVariant = textOrNull("transmitterGenerationVariant")
            displayApp = textOrNull("displayApp")
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun JsonNode.textOrNull(field: String): String? =
        get(field)?.takeIf { !it.isNull }?.asText()

    private fun JsonNode.intOrNull(field: String): Int? =
        get(field)?.takeIf { !it.isNull }?.asInt()

    private fun JsonNode.longOrNull(field: String): Long? =
        get(field)?.takeIf { !it.isNull }?.asLong()

    private fun JsonNode.doubleOrNull(field: String): Double? =
        get(field)?.takeIf { !it.isNull }?.asDouble()

    companion object {
        fun parseDexcomTime(value: String): Instant {
            return try {
                OffsetDateTime.parse(value).toInstant()
            } catch (_: DateTimeParseException) {
                Instant.parse(value)
            }
        }
    }
}