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
import org.apache.avro.specific.SpecificRecord
import org.radarbase.huawei.user.User
import java.time.Instant

/** Huawei's healthRecords v2 endpoint reports startTime/endTime in nanoseconds since the epoch. */
private fun JsonNode.epochInstant(field: String): Instant? {
    val value = this.get(field) ?: return null
    if (value.isNull) return null
    val nanos = if (value.isTextual) value.asText().toLongOrNull() else value.asLong()
    return nanos?.let { Instant.ofEpochSecond(it / 1_000_000_000L, it % 1_000_000_000L) }
}

/**
 * Generic converter for `GET /healthkit/v2/healthRecords` responses: iterates every record
 * returned for the requested `dataType` and builds one Avro record per entry via
 * [buildRecord].
 *
 * @author yatharthranjan
 */
class HuaweiHealthRecordConverter(
    private val topic: String,
    private val buildRecord: (
        fields: FieldValues,
        startTime: Instant,
        endTime: Instant?,
        timeReceived: Instant,
    ) -> SpecificRecord,
) : HuaweiDataConverter {

    override fun processRecords(root: JsonNode, user: User): Sequence<Result<TopicData>> {
        val timeReceived = Instant.now()
        val records = root.get("healthRecords") ?: root.get("records") ?: return emptySequence()
        return records.asSequence()
            .mapCatching { record ->
                val startTime = record.epochInstant("startTime")
                    ?: error("Huawei health record is missing startTime")
                val endTime = record.epochInstant("endTime")
                val fieldValues = FieldValues.from(
                    record.get("value") ?: record.get("fieldValues") ?: record.get("field"),
                )
                TopicData(
                    topic = topic,
                    key = user.observationKey,
                    offset = startTime.epochSecond,
                    value = buildRecord(fieldValues, startTime, endTime, timeReceived),
                )
            }
    }
}
