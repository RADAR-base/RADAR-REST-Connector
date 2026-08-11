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
import org.radarbase.googlehealth.util.googleHealthFloors

/**
 * Converts `floors` data points: elevation gained over an interval.
 * Google documents `count` as an int64, serialised as a JSON string, `asInt` parses either form.
 * The type supports true zeros, so a `count` of 0 is a real observation and is kept.
 */
class FloorsGoogleHealthAvroConverter(topic: String) : GoogleHealthAvroConverter(topic) {
    override fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>> {
        val data = point["floors"] ?: return emptyList()
        val (start, end) = parseInterval(data) ?: return emptyList()
        val count = data["count"]?.asInt() ?: return emptyList()
        val record = googleHealthFloors {
            time = epochSeconds(start)
            endTime = epochSeconds(end)
            timeReceived = nowEpochSeconds()
            floors = count
        }
        return listOf(user.observationKey to record)
    }
}
