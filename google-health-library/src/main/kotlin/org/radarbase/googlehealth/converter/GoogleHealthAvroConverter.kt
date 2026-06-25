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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

abstract class GoogleHealthAvroConverter(override val topic: String) : AvroConverter {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    abstract fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>>

    override fun convert(tree: JsonNode, user: User): List<Pair<SpecificRecord, SpecificRecord>> {
        val points = tree["dataPoints"] ?: tree["rollupDataPoints"] ?: return emptyList()
        if (!points.isArray) return emptyList()
        return points.flatMap { convertDataPoint(it, user) }
    }

    companion object {
        fun parseInterval(node: JsonNode): Pair<Instant, Instant>? {
            val interval = node["interval"] ?: return null
            val start = runCatching {
                Instant.parse(interval["startTime"]?.asText() ?: return null)
            }.getOrNull() ?: return null
            val end = runCatching {
                Instant.parse(interval["endTime"]?.asText() ?: return null)
            }.getOrNull() ?: return null
            return start to end
        }

        fun parseSampleTime(node: JsonNode): Instant? {
            val sample = node["sampleTime"] ?: return null
            val text = sample["physicalTime"]?.asText()?.takeIf { it.isNotEmpty() } ?: return null
            return runCatching { Instant.parse(text) }.getOrNull()
        }

        fun parseDate(node: JsonNode, utcOffsetSeconds: Int = 0): Instant? {
            val dateNode = node["date"] ?: return null
            val year = dateNode["year"]?.asInt() ?: return null
            val month = dateNode["month"]?.asInt() ?: return null
            val day = dateNode["day"]?.asInt() ?: return null
            val local = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return null
            val timeNode = node["time"]
            val dateTime = if (timeNode != null && !timeNode.isNull) {
                LocalDateTime.of(
                    local,
                    LocalTime.of(
                        timeNode["hours"]?.asInt() ?: 0,
                        timeNode["minutes"]?.asInt() ?: 0,
                        timeNode["seconds"]?.asInt() ?: 0,
                        timeNode["nanos"]?.asInt() ?: 0,
                    ),
                )
            } else {
                local.atStartOfDay()
            }
            return dateTime.toInstant(ZoneOffset.ofTotalSeconds(utcOffsetSeconds))
        }

        /** Parses a Google `Duration` string (e.g. "36s", "780s") to whole seconds. */
        fun parseDurationSeconds(durationText: String?): Int {
            if (durationText.isNullOrEmpty()) return 0
            val trimmed = durationText.trim().removeSuffix("s")
            val seconds = trimmed.toDoubleOrNull() ?: return 0
            return seconds.toInt()
        }

        /** A UTC offset is encoded as a [Duration]; returns its value in seconds. */
        fun parseUtcOffsetSeconds(offsetText: String?): Int = parseDurationSeconds(offsetText)

        fun epochSeconds(instant: Instant): Double = instant.toEpochMilli() / 1000.0

        fun nowEpochSeconds(): Double = Instant.now().toEpochMilli() / 1000.0
    }
}
