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
import org.radarbase.googlehealth.util.intradayCalories
import java.time.Instant

class TotalCaloriesGoogleHealthAvroConverter(topic: String) : GoogleHealthAvroConverter(topic) {
    override fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>> {
        val start = readInstant(point, "startTime")
            ?: point["totalCalories"]?.get("interval")?.get("startTime")?.asText()?.let(Instant::parse)
            ?: return emptyList()
        val end = readInstant(point, "endTime")
            ?: point["totalCalories"]?.get("interval")?.get("endTime")?.asText()?.let(Instant::parse)
            ?: return emptyList()
        val kilocalories = extractKilocalories(point) ?: return emptyList()
        val record = intradayCalories {
            time = epochSeconds(start)
            timeReceived = nowEpochSeconds()
            timeInterval = (end.epochSecond - start.epochSecond).toInt().coerceAtLeast(0)
            calories = kilocalories
            level = 0
            mets = 0.0
        }
        return listOf(user.observationKey to record)
    }

    private fun readInstant(node: JsonNode, field: String): Instant? =
        node[field]?.asText()?.takeIf { it.isNotEmpty() }?.let(Instant::parse)

    /** RollupDataPoint's `totalCalories.kcalSum` is the real field (confirmed live 2026-04-20);
     *  `kilocalories` is kept as a fallback for forward-compatibility in case the API changes. */
    private fun extractKilocalories(point: JsonNode): Double? {
        point["totalCalories"]?.get("kcalSum")?.takeIf { !it.isNull }
            ?.let { return it.doubleValue() }
        point["totalCalories"]?.get("kilocalories")?.takeIf { !it.isNull }
            ?.let { return it.doubleValue() }
        point["value"]?.get("totalCalories")?.get("kcalSum")?.takeIf { !it.isNull }
            ?.let { return it.doubleValue() }
        point["kilocalories"]?.takeIf { !it.isNull }?.let { return it.doubleValue() }
        return null
    }
}
