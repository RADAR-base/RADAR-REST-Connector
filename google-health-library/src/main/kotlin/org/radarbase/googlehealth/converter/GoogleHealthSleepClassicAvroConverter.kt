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
import org.radarbase.googlehealth.util.googleHealthSleepClassic
import org.radarcns.push.googlehealth.GoogleHealthSleepClassicLevel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class GoogleHealthSleepClassicAvroConverter(topic: String) : GoogleHealthAvroConverter(topic) {
    override fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>> {
        val data = point["sleep"] ?: return emptyList()
        val stages = data["stages"]?.takeIf { it.isArray } ?: return emptyList()
        if (SleepSession.familyOf(data) != SleepSessionFamily.CLASSIC) return emptyList()
        val timeReceived = nowEpochSeconds()
        return stages.mapNotNull { stage ->
            val stageType = stage["type"]?.asText() ?: return@mapNotNull null
            if (stageType !in CLASSIC_FAMILY) return@mapNotNull null
            val start = stage["startTime"]?.asText()
                ?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
            val end = stage["endTime"]?.asText()
                ?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
            // Render in the stage's own UTC offset so dateTime is the device's local wall clock
            // (like Fitbit), not UTC. Google derives its civil fields the same way (physical + offset).
            val startZone = ZoneOffset.ofTotalSeconds(
                parseUtcOffsetSeconds(stage["startUtcOffset"]?.asText()),
            )
            val record = googleHealthSleepClassic {
                dateTime = LOCAL_FMT.format(LocalDateTime.ofInstant(start, startZone))
                this.timeReceived = timeReceived
                duration = (end.epochSecond - start.epochSecond).toInt().coerceAtLeast(0)
                level = mapLevel(stageType)
            }
            user.observationKey to record
        }
    }

    private fun mapLevel(text: String?): GoogleHealthSleepClassicLevel = when (text) {
        "ASLEEP" -> GoogleHealthSleepClassicLevel.ASLEEP
        "RESTLESS" -> GoogleHealthSleepClassicLevel.RESTLESS
        "AWAKE" -> GoogleHealthSleepClassicLevel.AWAKE
        else -> GoogleHealthSleepClassicLevel.UNKNOWN
    }

    companion object {
        private val LOCAL_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        private val CLASSIC_FAMILY = setOf("ASLEEP", "RESTLESS", "AWAKE")
    }
}
