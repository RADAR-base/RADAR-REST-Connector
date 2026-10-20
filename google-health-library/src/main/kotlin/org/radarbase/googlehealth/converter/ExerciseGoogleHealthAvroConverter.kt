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
import org.radarbase.googlehealth.util.exerciseHeartRate
import org.radarbase.googlehealth.util.activityLogRecord

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
        val metrics = data["metricsSummary"]
        val distanceKm = metrics?.get("distanceMillimeters")?.takeIf { !it.isNull }
            ?.asDouble()?.let { it.toFloat() / 1_000_000f }
        val caloriesKcal = metrics?.get("caloriesKcal")?.takeIf { !it.isNull }?.asDouble()
        val energyKj = caloriesKcal?.let { (it * KCAL_TO_KJ).toFloat() }
        val stepCount = metrics?.get("steps")?.takeIf { !it.isNull }?.asInt()
        val avgHr = metrics?.get("averageHeartRateBeatsPerMinute")?.takeIf { !it.isNull }?.asInt()
        val avgHeartRate = avgHr?.let { exerciseHeartRate { mean = it } }
        val exerciseType = data["exerciseType"]?.asText()

        val activityId = point["dataPointName"]?.asText()?.substringAfterLast('/')?.toLongOrNull()
            ?: throw IllegalStateException("Exercise data point has no usable dataPointName log id: $point")
        val record = activityLogRecord {
            time = epochSeconds(start)
            timeReceived = nowEpochSeconds()
            timeZoneOffset = offsetSeconds
            timeLastModified = epochSeconds(end)
            duration = durationSec
            durationActive = durationSec
            id = activityId
            name = data["displayName"]?.asText() ?: exerciseType
            logType = point["dataSource"]?.get("recordingMethod")?.asText()
            type = null
            source = null
            manualDataEntry = null
            energy = energyKj
            levels = null
            heartRate = avgHeartRate
            steps = stepCount
            distance = distanceKm
            speed = null
        }
        return listOf(user.observationKey to record)
    }

    companion object {
        private const val KCAL_TO_KJ = 4.1868
    }
}
