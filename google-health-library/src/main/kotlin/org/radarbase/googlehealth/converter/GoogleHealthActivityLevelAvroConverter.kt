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
import org.radarbase.googlehealth.util.googleHealthActivityLevel
import org.radarcns.push.googlehealth.GoogleHealthActivityLevelType

/**
 * Converts `activity-level` data points: the activity level the user sustained over an interval,
 * one minute long in practice, reported for every interval of the day. The sedentary intervals are
 * also reported grouped into longer periods, see [GoogleHealthSedentaryPeriodAvroConverter].
 */
class GoogleHealthActivityLevelAvroConverter(topic: String) : GoogleHealthAvroConverter(topic) {
    override fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>> {
        val data = point["activityLevel"] ?: return emptyList()
        val (start, end) = parseInterval(data) ?: return emptyList()
        val record = googleHealthActivityLevel {
            time = epochSeconds(start)
            timeReceived = nowEpochSeconds()
            timeInterval = (end.epochSecond - start.epochSecond).toInt().coerceAtLeast(0)
            level = mapLevel(data["activityLevelType"]?.asText())
        }
        return listOf(user.observationKey to record)
    }

    /**
     * Google's `ACTIVITY_LEVEL_TYPE_UNSPECIFIED` is kept as its own symbol, it means Google itself
     * did not classify the interval. Any other symbol, including ones Google adds later, is
     * `UNKNOWN`.
     */
    private fun mapLevel(text: String?): GoogleHealthActivityLevelType = when (text) {
        "SEDENTARY" -> GoogleHealthActivityLevelType.SEDENTARY
        "LIGHTLY_ACTIVE" -> GoogleHealthActivityLevelType.LIGHTLY_ACTIVE
        "MODERATELY_ACTIVE" -> GoogleHealthActivityLevelType.MODERATELY_ACTIVE
        "VERY_ACTIVE" -> GoogleHealthActivityLevelType.VERY_ACTIVE
        "ACTIVITY_LEVEL_TYPE_UNSPECIFIED" ->
            GoogleHealthActivityLevelType.ACTIVITY_LEVEL_TYPE_UNSPECIFIED
        else -> GoogleHealthActivityLevelType.UNKNOWN
    }
}
