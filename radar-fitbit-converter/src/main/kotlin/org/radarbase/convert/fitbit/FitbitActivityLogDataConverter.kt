/*
 * Copyright 2018 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.radarbase.convert.fitbit

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.fitbit.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class FitbitActivityLogDataConverter(
    private val activityLogTopic: String
) : FitbitDataConverter {
    override fun processRecords(
        context: ConverterContext, root: JsonNode, timeReceived: Double
    ): Sequence<Result<TopicData>> {
        val array = root.optArray("activities")
            ?: return emptySequence()

        return array.asSequence()
            .sortedBy { it["startTime"].textValue() }
            .mapCatching { s ->
                val startTime = OffsetDateTime.parse(s["startTime"].textValue())
                val startInstant = startTime.toInstant()
                TopicData(
                    sourceOffset = startInstant,
                    topic = activityLogTopic,
                    value = s.toActivityLogRecord(startInstant, startTime.offset),
                )
            }
    }

    private fun JsonNode.toActivityLogRecord(
        startTime: Instant,
        offset: ZoneOffset,
    ): FitbitActivityLogRecord {
        return FitbitActivityLogRecord.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            timeLastModified = Instant.parse(get("lastModified").asText()).toEpochMilli() / 1000.0
            id = requireNotNull(optLong("logId")) { "Activity log ID not specified" }
            logType = optString("logType")
            type = optLong("activityType")
            speed = optDouble("speed")
            distance = optDouble("distance")?.toFloat()
            steps = optInt("steps")
            energy = optInt("calories")?.let { it * FOOD_CAL_TO_KJOULE_FACTOR }
            duration = (requireNotNull(optLong("duration")) { "Activity log duration not specified" }
                / 1000f)
            durationActive = requireNotNull(optLong("durationActive")) { "Activity active log duration not specified" } / 1000f
            timeZoneOffset = offset.totalSeconds
            name = optString("activityName")
            heartRate = toHeartRate()
            manualDataEntry = optObject("manualValuesSpecified")?.toManualDataEntry()
            levels = optArray("activityLevels")?.toActivityLevels()
            source = optObject("source")?.toSource()
        }.build()
    }

    private fun JsonNode.toSource(): FitbitSource? =
        optString("id")?.let { sourceId ->
            FitbitSource.newBuilder().apply {
                id = sourceId
                name = optString("name")
                type = optString("type")
                url = optString("url")
            }.build()
        }

    private fun Iterable<JsonNode>.toActivityLevels(): FitbitActivityLevels =
        FitbitActivityLevels.newBuilder().apply {
            forEach { level ->
                val durationMinutes = level.optInt("minutes") ?: return@forEach
                val duration = durationMinutes * 60
                when (level.optString("name")) {
                    "sedentary" -> durationSedentary = duration
                    "lightly" -> durationLightly = duration
                    "fairly" -> durationFairly = duration
                    "very" -> durationVery = duration
                }
            }
        }.build()

    private fun JsonNode.toManualDataEntry(): FitbitManualDataEntry =
        FitbitManualDataEntry.newBuilder().apply {
            steps = optBoolean("steps")
            distance = optBoolean("distance")
            energy = optBoolean("calorie")
        }.build()

    private fun JsonNode.toHeartRate(): FitbitActivityHeartRate? {
        val averageHeartRate: Int? = optInt("averageHeartRate")
        val zones = optArray("heartRateZones")
        if (averageHeartRate == null && zones == null) {
            return null
        }
        return FitbitActivityHeartRate.newBuilder().apply {
            mean = averageHeartRate
            zones?.forEach { zone ->
                val minValue = zone.optInt("min")
                val duration = zone.optInt("minutes")?.let { it * 60 }
                when (val zoneText = zone.optString("name")) {
                    "Out of Range" -> {
                        min = minValue
                        durationOutOfRange = duration
                    }
                    "Fat Burn" -> {
                        minFatBurn = minValue
                        durationFatBurn = duration
                    }
                    "Cardio" -> {
                        minCardio = minValue
                        durationCardio = duration
                    }
                    "Peak" -> {
                        minPeak = minValue
                        max = zone.optInt("max")
                        durationPeak = duration
                    }
                    else -> logger.warn("Cannot process unknown heart rate zone {}", zoneText)
                }
            }
        }.build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FitbitActivityLogDataConverter::class.java)
        private const val FOOD_CAL_TO_KJOULE_FACTOR = 4.1868f
    }
}
