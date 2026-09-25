package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.dexcom.user.User
import org.radarcns.connector.dexcom.DexcomDisplayDevice
import org.radarcns.connector.dexcom.DexcomEgv
import org.radarcns.connector.dexcom.DexcomEgvStatus
import org.radarcns.connector.dexcom.DexcomEgvTrend
import org.radarcns.connector.dexcom.DexcomTransmitterGeneration
import org.radarcns.connector.dexcom.DexcomTransmitterGenerationVariant
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
            time = systemTimeInstant.epochSecond.toDouble()
            displayTime = textOrNull("displayTime")
            transmitterId = textOrNull("transmitterId")
            transmitterTicks = longOrNull("transmitterTicks")
            value = intOrNull("value")
            status = parseStatus(textOrNull("status"))
            trend = parseTrend(textOrNull("trend"))
            trendRate = doubleOrNull("trendRate")
            unit = textOrNull("unit") ?: "mg/dL"
            rateUnit = textOrNull("rateUnit") ?: "mg/dL/min"
            displayDevice = parseDisplayDevice(textOrNull("displayDevice"))
            transmitterGeneration = parseTransmitterGeneration(textOrNull("transmitterGeneration"))
            transmitterGenerationVariant =
                parseTransmitterGenerationVariant(textOrNull("transmitterGenerationVariant"))
            displayApp = textOrNull("displayApp")
            timeReceived = System.currentTimeMillis() / 1000.0
        }.build()

    private fun parseStatus(value: String?): DexcomEgvStatus? =
        when (value) {
            null -> null
            else -> when (value.lowercase()) {
                "high" -> DexcomEgvStatus.HIGH
                "low" -> DexcomEgvStatus.LOW
                else -> DexcomEgvStatus.UNKNOWN
            }
        }

    private fun parseTrend(value: String?): DexcomEgvTrend? =
        when (value) {
            null -> null
            else -> when (value.lowercase()) {
                "none" -> DexcomEgvTrend.NONE
                "doubleup" -> DexcomEgvTrend.DOUBLE_UP
                "singleup" -> DexcomEgvTrend.SINGLE_UP
                "fortyfiveup" -> DexcomEgvTrend.FORTY_FIVE_UP
                "flat" -> DexcomEgvTrend.FLAT
                "fortyfivedown" -> DexcomEgvTrend.FORTY_FIVE_DOWN
                "singledown" -> DexcomEgvTrend.SINGLE_DOWN
                "doubledown" -> DexcomEgvTrend.DOUBLE_DOWN
                "notcomputable" -> DexcomEgvTrend.NOT_COMPUTABLE
                "rateoutofrange" -> DexcomEgvTrend.RATE_OUT_OF_RANGE
                else -> DexcomEgvTrend.UNKNOWN
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

    private fun JsonNode.intOrNull(field: String): Int? =
        get(field)?.takeIf { !it.isNull }?.asInt()

    private fun JsonNode.longOrNull(field: String): Long? =
        get(field)?.takeIf { !it.isNull }?.asLong()

    private fun JsonNode.doubleOrNull(field: String): Double? =
        get(field)?.takeIf { !it.isNull && it.isNumber }?.asDouble()

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