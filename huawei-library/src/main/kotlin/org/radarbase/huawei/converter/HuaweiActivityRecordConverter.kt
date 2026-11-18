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
 * Converts `GET /healthkit/v2/activityRecords` responses into [HuaweiActivityRecord]s.
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
        val records = root.get("activityRecord")
            ?: root.get("activityRecords")
            ?: root.get("records")
            ?: return emptySequence()
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
        val fields = FieldValues.from(this)
        val device = this.get("device")?.let { FieldValues.from(it) }
        val summary = this.get("activitySummary")?.let { FieldValues.from(it) }
        return HuaweiActivityRecord.newBuilder().apply {
            time = startTime.toEpoch()
            this.timeReceived = timeReceived.toEpoch()
            endTime = epochInstant("endTime")?.toEpoch()
            activityRecordId = fields.getString("id", "activityRecordId")
            name = fields.getString("name")
            description = fields.getString("desc", "description")
            timeZone = fields.getString("timeZone")
            activityTypeId = fields.getString("activityType", "activityTypeId")
            activeTimeMillis = fields.getLong("activeTime", "activeTimeMillis")
            isKeepGoing = this@toActivityRecord.get("isKeepGoing")
                ?.takeIf { it.isBoolean }
                ?.asBoolean()
            deviceManufacturer = device?.getString("manufacturer")
            deviceType = device?.getInt("type")
            activitySummaryAvgPace = summary?.getDouble("avgPace")
            activitySummaryBestPace = summary?.getDouble("bestPace")
            activitySummaryPaceMap = summary?.getString("paceMap")
            activitySummaryDataSummary = summary?.getString("dataSummary")
            activitySummarySectionSummary = summary?.getString("sectionSummary")
        }.build()
    }
}
