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
import org.radarcns.connector.huawei.HuaweiContinuousSleepFragment
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
    fun `reads recorded sample points with nanosecond times`() {
        val root = mapper.readTree(
            checkNotNull(javaClass.getResource("/huawei-responses/continuous_steps_delta.json")),
        )
        val records = converter.processRecords(root, user).map { it.getOrThrow() }.toList()
        val values = records.map { it.value as HuaweiContinuousStepsDelta }

        assertEquals(listOf(1795014300L, 1795014360L), records.map { it.offset })
        assertEquals(listOf(1795014300.0, 1795014360.0), values.map { it.time })
        assertEquals(listOf(1795014360.0, 1795014420.0), values.map { it.endTime })
        assertEquals(listOf(60, 41), values.map { it.stepsDelta })
    }

    @Test
    fun `skips points of other data types`() {
        // A recorded sleep fragment response also carries the night's sleep record point.
        val root = mapper.readTree(
            checkNotNull(javaClass.getResource("/huawei-responses/continuous_sleep_fragment.json")),
        )
        val fragments = HuaweiSampleSetConverter(
            "topic",
            "com.huawei.continuous.sleep.fragment",
        ) { f, start, end, received ->
            HuaweiContinuousSleepFragment.newBuilder().apply {
                time = start.toEpochMilli() / 1000.0
                timeReceived = received.toEpochMilli() / 1000.0
                endTime = end?.let { it.toEpochMilli() / 1000.0 }
                sleepState = f.getInt("sleep_state")
            }.build()
        }.processRecords(root, user).map { it.getOrThrow().value as HuaweiContinuousSleepFragment }
            .toList()

        assertEquals(listOf(4, 4), fragments.map { it.sleepState })
    }

    @Test
    fun `infers timestamp units from magnitude`() {
        // Group times are milliseconds and sample point times nanoseconds in live responses.
        val expected = Instant.parse("2026-11-18T15:05:00Z")
        assertEquals(expected, epochInstantOf(1795014300000L))
        assertEquals(expected, epochInstantOf(1795014300000000000L))
    }
}
