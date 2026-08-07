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
import org.radarbase.googlehealth.util.googleHealthDailyRestingHeartRate

class DailyRestingHeartRateGoogleHealthAvroConverter(topic: String) :
    GoogleHealthAvroConverter(topic) {
    override fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>> {
        val data = point["dailyRestingHeartRate"] ?: return emptyList()
        val dateNode = data["date"] ?: return emptyList()
        val bpm = data["beatsPerMinute"]?.asInt() ?: return emptyList()
        val isoDate = String.format(
            "%04d-%02d-%02d",
            dateNode["year"].asInt(),
            dateNode["month"].asInt(),
            dateNode["day"].asInt(),
        )
        val record = googleHealthDailyRestingHeartRate {
            date = isoDate
            timeReceived = nowEpochSeconds()
            restingHeartRate = bpm
        }
        return listOf(user.observationKey to record)
    }
}
