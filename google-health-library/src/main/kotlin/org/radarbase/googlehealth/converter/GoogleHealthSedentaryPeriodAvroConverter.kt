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
import org.radarbase.googlehealth.util.googleHealthSedentaryPeriod

/**
 * Converts `sedentary-period` data points: stretches during which the user was not moving while
 * wearing the device. Google groups each unbroken run of sedentary time into one point, so the
 * points vary in length from minutes to hours and only cover the sedentary parts of the day.
 * `activity-level` carries the same signal per minute, see
 * [GoogleHealthActivityLevelAvroConverter]. Google documents `interval` as this type's only field,
 * so the record carries the start and end of that interval alone.
 */
class GoogleHealthSedentaryPeriodAvroConverter(topic: String) : GoogleHealthAvroConverter(topic) {
    override fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>> {
        val data = point["sedentaryPeriod"] ?: return emptyList()
        val (start, end) = parseInterval(data) ?: return emptyList()
        val record = googleHealthSedentaryPeriod {
            time = epochSeconds(start)
            endTime = epochSeconds(end)
            timeReceived = nowEpochSeconds()
        }
        return listOf(user.observationKey to record)
    }
}
