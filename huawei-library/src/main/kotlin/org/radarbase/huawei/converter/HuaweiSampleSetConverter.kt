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

/**
 * Generic converter for `sampleSet:polymerize` and `sampleSet:dailyPolymerize` responses:
 * iterates every sample point of every sample set in the response and builds one Avro record per
 * point via [buildRecord].
 *
 * Huawei wraps sample sets in a `group[]` array (one entry per aggregation bucket or day), each
 * holding `sampleSet[].samplePoints[]`. A bare top-level `sampleSet[]` is also accepted. Sample
 * point times are nanoseconds in Huawei's examples, while group times are milliseconds; units are
 * inferred per value (see [epochInstant]).
 *
 * This single converter is reused for the large majority of Huawei Health Kit data types, since
 * they all share the same response envelope and differ only in which Avro record type their field
 * values are mapped onto.
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
        return root.samplePoints()
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

    companion object {
        private fun JsonNode.child(vararg names: String): JsonNode? =
            names.firstNotNullOfOrNull { name -> get(name)?.takeIf { it.isArray } }

        /** All sample points in a (daily) polymerize response, with or without `group[]`. */
        internal fun JsonNode.samplePoints(): Sequence<JsonNode> {
            val sampleSets = child("group", "groups")
                ?.asSequence()
                ?.flatMap { group ->
                    group.child("sampleSet", "sampleSets")?.asSequence().orEmpty()
                }
                ?: child("sampleSet", "sampleSets")?.asSequence()
                ?: emptySequence()
            return sampleSets.flatMap { sampleSet ->
                sampleSet.child("samplePoints", "samplePoint")?.asSequence().orEmpty()
            }
        }
    }
}
