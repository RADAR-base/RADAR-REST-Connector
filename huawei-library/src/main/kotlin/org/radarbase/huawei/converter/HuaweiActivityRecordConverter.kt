/*
 * Copyright 2026 Onsentia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.radarbase.huawei.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.huawei.user.User
import org.radarcns.connector.huawei.HuaweiActivityRecord
import java.time.Instant

/**
 * Converts `GET /healthkit/v1/activityRecords` responses into [HuaweiActivityRecord]s.
 *
 * Field names below follow the Huawei Health Kit `ActivityRecord`/`Device`/`ActivitySummary`
 * model (activity record id, name, description, time zone, activity type, device manufacturer and
 * type, and a nested activity summary with pace/data/section statistics). Nested JSON structures
 * that map to free-form Avro `string` fields (pace map, data summary, section summary) are kept as
 * their raw JSON text, since their internal shape varies by activity type.
 *
 * @author yatharthranjan
 */
class HuaweiActivityRecordConverter(
    private val topic: String = "connect_huawei_activity_record",
) : HuaweiDataConverter {

    override fun processRecords(root: JsonNode, user: User): Sequence<Result<TopicData>> {
        val timeReceived = Instant.now()
        val records = root.get("activityRecords") ?: root.get("records") ?: return emptySequence()
        return records.asSequence()
            .mapCatching { record ->
                val startTime = record.epochInstant("startTime")
                    ?: error("Huawei activity record is missing startTime")
                TopicData(
                    topic = topic,
                    key = user.observationKey,
                    offset = startTime.epochSecond,
                    value = record.toActivityRecord(startTime, timeReceived),
                )
            }
    }

    private fun JsonNode.toActivityRecord(
        startTime: Instant,
        timeReceived: Instant,
    ): HuaweiActivityRecord {
        val device = this.get("device")
        val summary = this.get("activitySummary")
        return HuaweiActivityRecord.newBuilder().apply {
            time = startTime.toEpoch()
            this.timeReceived = timeReceived.toEpoch()
            endTime = epochInstant("endTime")?.toEpoch()
            activityRecordId = textOrNull("id") ?: textOrNull("activityRecordId")
            name = textOrNull("name")
            description = textOrNull("description")
            timeZone = textOrNull("timeZone")
            activityTypeId = textOrNull("activityType") ?: textOrNull("activityTypeId")
            activeTimeMillis = longOrNull("activeTime") ?: longOrNull("activeTimeMillis")
            isKeepGoing = boolOrNull("isKeepGoing")
            deviceManufacturer = device?.textOrNull("manufacturer")
            deviceType = device?.intOrNull("type")
            activitySummaryAvgPace = summary?.doubleOrNull("avgPace")
            activitySummaryBestPace = summary?.doubleOrNull("bestPace")
            activitySummaryPaceMap = summary?.get("paceMap")?.toString()
            activitySummaryDataSummary = summary?.get("dataSummary")?.toString()
            activitySummarySectionSummary = summary?.get("sectionSummary")?.toString()
        }.build()
    }

    private fun JsonNode.epochInstant(field: String): Instant? {
        val value = this.get(field) ?: return null
        if (value.isNull) return null
        val millis = if (value.isTextual) value.asText().toLongOrNull() else value.asLong()
        return millis?.let { Instant.ofEpochMilli(it) }
    }

    private fun JsonNode.textOrNull(field: String): String? =
        this.get(field)?.takeUnless { it.isNull }?.asText()

    private fun JsonNode.intOrNull(field: String): Int? =
        this.get(field)?.takeUnless { it.isNull }?.asInt()

    private fun JsonNode.longOrNull(field: String): Long? =
        this.get(field)?.takeUnless { it.isNull }?.asLong()

    private fun JsonNode.doubleOrNull(field: String): Double? =
        this.get(field)?.takeUnless { it.isNull }?.asDouble()

    private fun JsonNode.boolOrNull(field: String): Boolean? =
        this.get(field)?.takeUnless { it.isNull }?.asBoolean()
}
