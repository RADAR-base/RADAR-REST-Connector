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
import org.radarbase.googlehealth.util.googleHealthIrregularRhythmNotification
import java.io.IOException
import java.time.Instant

/**
 * Emits one record per heart beat of an Irregular Rhythm Notification, flattening the API's
 * session -> alertWindow -> heartBeat hierarchy. Each record carries the heart beat's own time
 * plus the context of its parent window (start/end, positive) and session (start), with the
 * device metadata repeated, linked by the shared notification id.
 */
class GoogleHealthIrregularRhythmNotificationAvroConverter(
    topic: String,
) : GoogleHealthAvroConverter(topic) {
    override fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>> {
        val data = point["irregularRhythmNotification"] ?: return emptyList()
        val id = (point["name"] ?: point["dataPointName"])?.asText()?.substringAfterLast('/')
            ?: throw IOException(
                "Irregular rhythm notification data point has no name or dataPointName to derive " +
                    "an id from for user=${user.versionedId}",
            )
        val windows = data["alertWindows"]?.takeIf { it.isArray } ?: return emptyList()

        val device = data["medicalDeviceInfo"]
        val sessionStart = data["interval"]?.get("startTime")?.asText()
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val firmwareVersion = device?.get("firmwareVersion")?.asText()
        val featureVersion = device?.get("featureVersion")?.asText()
        val deviceModel = device?.get("deviceModel")?.asText()
        val received = nowEpochSeconds()

        return windows.flatMap { window ->
            val windowStart = window["startTime"]?.asText()
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            val windowEnd = window["endTime"]?.asText()
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            if (windowStart == null || windowEnd == null) return@flatMap emptyList()
            val positive = window["positive"]?.takeIf { !it.isNull }?.asBoolean()
            val heartBeats = window["heartBeats"]?.takeIf { it.isArray }
                ?: return@flatMap emptyList()

            heartBeats.mapNotNull { beat ->
                val beatTime = (beat["physicalTime"] ?: beat["time"])?.asText()
                    ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    ?: return@mapNotNull null
                val record = googleHealthIrregularRhythmNotification {
                    time = epochSeconds(beatTime)
                    timeReceived = received
                    this.id = id
                    sessionStartTime = sessionStart?.let { epochSeconds(it) }
                    windowStartTime = epochSeconds(windowStart)
                    windowEndTime = epochSeconds(windowEnd)
                    this.positive = positive
                    beatsPerMinute = beat["beatsPerMinute"]?.takeIf { !it.isNull }?.asText()
                        ?.toIntOrNull()
                    this.firmwareVersion = firmwareVersion
                    this.featureVersion = featureVersion
                    this.deviceModel = deviceModel
                }
                user.observationKey to record
            }
        }
    }
}
