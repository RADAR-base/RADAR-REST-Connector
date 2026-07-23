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

private fun JsonNode.epochInstant(field: String): Instant? {
    val value = this.get(field) ?: return null
    if (value.isNull) return null
    val millis = if (value.isTextual) value.asText().toLongOrNull() else value.asLong()
    return millis?.let { Instant.ofEpochMilli(it) }
}

/**
 * Generic converter for `sampleSet:polymerize` responses: iterates every sample point of every
 * data-type group in the response and builds one Avro record per point via [buildRecord].
 *
 * This single converter is reused for the large majority of Huawei Health Kit data types, since
 * they all share the same `sampleSet[].samplePoints[]` response envelope and differ only in which
 * Avro record type their field values are mapped onto.
 *
 * @author yatharthranjan
 */
class HuaweiSampleSetConverter(
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
        val sampleSets = root.get("sampleSet") ?: root.get("sampleSets") ?: return emptySequence()
        return sampleSets.asSequence()
            .flatMap { group ->
                val points = group.get("samplePoints") ?: group.get("samplePoint")
                points?.asSequence() ?: emptySequence()
            }
            .mapCatching { point ->
                val startTime = point.epochInstant("startTime")
                    ?: error("Huawei sample point is missing startTime")
                val endTime = point.epochInstant("endTime")
                val fieldValues = FieldValues.from(point.get("value") ?: point.get("fieldValues"))
                TopicData(
                    topic = topic,
                    key = user.observationKey,
                    offset = startTime.epochSecond,
                    value = buildRecord(fieldValues, startTime, endTime, timeReceived),
                )
            }
    }
}
