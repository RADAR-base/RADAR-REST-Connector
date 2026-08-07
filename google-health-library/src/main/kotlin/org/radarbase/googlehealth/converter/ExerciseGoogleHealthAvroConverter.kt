/*
 * Copyright 2026 King's College London
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.googlehealth.converter

import com.fasterxml.jackson.databind.JsonNode
import org.apache.avro.specific.SpecificRecord
import org.radarbase.googlehealth.user.User
import org.radarbase.googlehealth.util.googleHealthExerciseHeartRate
import org.radarbase.googlehealth.util.googleHealthExercise
import org.radarbase.googlehealth.util.googleHealthSource
import java.time.Instant

class ExerciseGoogleHealthAvroConverter(topic: String) : GoogleHealthAvroConverter(topic) {
    override fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>> {
        val data = point["exercise"] ?: return emptyList()
        val (start, end) = parseInterval(data) ?: return emptyList()
        val offsetSeconds = parseUtcOffsetSeconds(
            data["interval"]?.get("startUtcOffset")?.asText(),
        )
        val durationSec = (end.epochSecond - start.epochSecond).toFloat().coerceAtLeast(0.0f)
        val activeDurationSec = data["activeDuration"]?.asText()
            ?.let { parseDurationSeconds(it).toFloat() } ?: durationSec
        val lastModified = data["updateTime"]?.asText()
            ?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: end
        val metrics = data["metricsSummary"]

        val distanceKm = metrics?.get("distanceMillimeters")?.takeIf { !it.isNull }
            ?.asText()?.toDoubleOrNull()?.let { it.toFloat() / 1_000_000f }

        val caloriesKcal = metrics?.get("caloriesKcal")?.takeIf { !it.isNull }?.asText()?.toDoubleOrNull()
        val energyKj = caloriesKcal?.let { (it * KCAL_TO_KJ).toFloat() }
        val stepCount = metrics?.get("steps")?.takeIf { !it.isNull }?.asText()?.toIntOrNull()

        val speedKmh = metrics?.get("averageSpeedMillimetersPerSecond")?.takeIf { !it.isNull }
            ?.asText()?.toDoubleOrNull()?.let { it * MM_PER_S_TO_KM_PER_H }

        val avgHr = metrics?.get("averageHeartRateBeatsPerMinute")?.takeIf { !it.isNull }?.asText()?.toIntOrNull()
        val zones = metrics?.get("heartRateZoneDurations")?.takeIf { !it.isNull }
        val avgHeartRate = if (avgHr != null || zones != null) {
            googleHealthExerciseHeartRate {
                mean = avgHr
                durationLight = zones?.get("lightTime")?.asText()?.let { parseDurationSeconds(it) }
                durationModerate = zones?.get("moderateTime")?.asText()?.let { parseDurationSeconds(it) }
                durationVigorous = zones?.get("vigorousTime")?.asText()?.let { parseDurationSeconds(it) }
                durationPeak = zones?.get("peakTime")?.asText()?.let { parseDurationSeconds(it) }
            }
        } else {
            null
        }
        val exerciseType = data["exerciseType"]?.asText()
        val dataSource = point["dataSource"]?.takeIf { !it.isNull }
        val device = dataSource?.get("device")
        val exerciseSource = dataSource?.let {
            googleHealthSource {
                name = device?.get("displayName")?.asText()
                formFactor = device?.get("formFactor")?.asText()
                manufacturer = device?.get("manufacturer")?.asText()
                platform = it["platform"]?.asText()
            }
        }

        val activityId = (point["name"] ?: point["dataPointName"])?.asText()
            ?.substringAfterLast('/')?.toLongOrNull()
            ?: run {
                logger.warn("Dropping exercise data point with no usable log id for user={}", user.versionedId)
                return emptyList()
            }

        val record = googleHealthExercise {
            time = epochSeconds(start)
            timeReceived = nowEpochSeconds()
            timeZoneOffset = offsetSeconds
            timeLastModified = epochSeconds(lastModified)
            duration = durationSec
            durationActive = activeDurationSec
            id = activityId
            name = data["displayName"]?.asText() ?: exerciseType
            logType = dataSource?.get("recordingMethod")?.asText()
            type = exerciseType
            source = exerciseSource
            energy = energyKj
            heartRate = avgHeartRate
            steps = stepCount
            distance = distanceKm
            speed = speedKmh
        }
        return listOf(user.observationKey to record)
    }

    companion object {
        private const val KCAL_TO_KJ = 4.1868
        private const val MM_PER_S_TO_KM_PER_H = 0.0036
    }
}
