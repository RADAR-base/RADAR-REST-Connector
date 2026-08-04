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

/** Sample points inside `sampleSet:dailyPolymerize`'s response report their times in nanoseconds. */
private fun JsonNode.epochNanoInstant(field: String): Instant? {
    val value = this.get(field) ?: return null
    if (value.isNull) return null
    val nanos = if (value.isTextual) value.asText().toLongOrNull() else value.asLong()
    return nanos?.let { Instant.ofEpochSecond(it / 1_000_000_000L, it % 1_000_000_000L) }
}

/**
 * Converter for `POST /healthkit/v2/sampleSet:dailyPolymerize` responses: unlike
 * `sampleSet:polymerize`, each day's result is wrapped in a `group[]` entry containing its own
 * `sampleSet[].samplePoints[]`, so this walks two levels of nesting instead of one before reaching
 * the same `{"fieldName": ..., "value": ...}` point shape used elsewhere.
 *
 * @author yatharthranjan
 */
class HuaweiDailyPolymerizeConverter(
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
        val groups = root.get("group") ?: return emptySequence()
        return groups.asSequence()
            .flatMap { group -> group.get("sampleSet")?.asSequence() ?: emptySequence() }
            .flatMap { sampleSet -> sampleSet.get("samplePoints")?.asSequence() ?: emptySequence() }
            .mapCatching { point ->
                val startTime = point.epochNanoInstant("startTime")
                    ?: error("Huawei daily polymerize sample point is missing startTime")
                val endTime = point.epochNanoInstant("endTime")
                val fieldValues = FieldValues.from(point.get("value"))
                TopicData(
                    topic = topic,
                    key = user.observationKey,
                    offset = startTime.epochSecond,
                    value = buildRecord(fieldValues, startTime, endTime, timeReceived),
                )
            }
    }
}
