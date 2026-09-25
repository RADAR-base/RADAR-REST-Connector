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

import com.fasterxml.jackson.databind.ObjectMapper
import org.radarbase.huawei.user.HuaweiUser
import org.radarcns.connector.huawei.HuaweiContinuousStepsDelta
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author yatharthranjan
 */
class HuaweiSampleSetConverterTest {
    private val mapper = ObjectMapper()
    private val user = HuaweiUser(
        id = "u1",
        createdAt = Instant.now(),
        projectId = "p",
        userId = "u",
        humanReadableUserId = null,
        sourceId = "s",
        externalId = "ext",
        isAuthorized = true,
        startDate = Instant.parse("2024-01-01T00:00:00Z"),
    )
    private val converter = HuaweiSampleSetConverter("topic") { f, start, end, received ->
        HuaweiContinuousStepsDelta.newBuilder().apply {
            time = start.toEpochMilli() / 1000.0
            timeReceived = received.toEpochMilli() / 1000.0
            endTime = end?.let { it.toEpochMilli() / 1000.0 }
            stepsDelta = f.getInt("steps_delta")
        }.build()
    }

    @Test
    fun `reads group-wrapped sample sets with nanosecond times`() {
        val root = mapper.readTree(
            """
            {"group": [{
              "startTime": 1704067200000, "endTime": 1704153600000,
              "sampleSet": [{"samplePoints": [
                {"startTime": 1704067200000000000, "endTime": "1704067260000000000",
                 "value": [{"fieldName": "steps_delta", "integerValue": 12}]}
              ]}]
            }]}
            """.trimIndent(),
        )
        val record = converter.processRecords(root, user).single().getOrThrow()
        val value = record.value as HuaweiContinuousStepsDelta

        assertEquals(1704067200L, record.offset)
        assertEquals(1704067200.0, value.time)
        assertEquals(1704067260.0, value.endTime)
        assertEquals(12, value.stepsDelta)
    }

    @Test
    fun `reads top-level sample sets with millisecond times`() {
        val root = mapper.readTree(
            """
            {"sampleSet": [{"samplePoints": [
              {"startTime": 1704067200000, "endTime": 1704067260000,
               "value": [{"fieldName": "steps_delta", "integerValue": 3}]}
            ]}]}
            """.trimIndent(),
        )
        val record = converter.processRecords(root, user).single().getOrThrow()

        assertEquals(1704067200L, record.offset)
    }

    @Test
    fun `infers timestamp units from magnitude`() {
        val expected = Instant.parse("2024-01-01T00:00:00.123Z")
        assertEquals(expected, epochInstantOf(1704067200123L))
        assertEquals(expected, epochInstantOf(1704067200123000L))
        assertEquals(expected, epochInstantOf(1704067200123000000L))
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), epochInstantOf(1704067200L))
    }
}
