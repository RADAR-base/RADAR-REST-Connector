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

package org.radarbase.connect.rest.huawei

import org.junit.jupiter.api.Test
import org.radarbase.huawei.converter.TopicData
import org.radarcns.connector.huawei.HuaweiContinuousStepsDelta
import org.radarcns.kafka.ObservationKey
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HuaweiSourceTaskTest {
    private val partition = mapOf<String, Any>("user" to "u1", "route" to "huawei_steps")
    private val key = ObservationKey("p", "u", "s")
    private val before = Instant.parse("2024-01-01T00:00:00Z")

    private fun record(offset: Instant) = TopicData(
        "topic",
        key,
        HuaweiContinuousStepsDelta.newBuilder().apply {
            time = offset.epochSecond.toDouble()
            timeReceived = offset.epochSecond.toDouble()
        }.build(),
        offset.epochSecond,
    )

    @Test
    fun `empty window that advanced the offset stores it in a heartbeat`() {
        val after = Instant.parse("2024-01-31T00:00:00Z")

        val heartbeat = HuaweiSourceTask.heartbeat("hb", partition, key, before, after, listOf())

        assertEquals("hb", heartbeat?.topic())
        assertEquals(partition, heartbeat?.sourcePartition())
        assertEquals(mapOf("timestamp" to after.epochSecond), heartbeat?.sourceOffset())
        assertNull(heartbeat?.value())
    }

    @Test
    fun `no heartbeat when the offset did not move`() {
        assertNull(HuaweiSourceTask.heartbeat("hb", partition, key, before, before, listOf()))
    }

    @Test
    fun `no heartbeat when the records already store the offset`() {
        val latest = Instant.parse("2024-01-05T00:00:00Z")

        val heartbeat = HuaweiSourceTask.heartbeat(
            "hb",
            partition,
            key,
            before,
            latest.plusSeconds(1),
            listOf(record(latest)),
        )

        assertNull(heartbeat)
    }

    @Test
    fun `heartbeat when a settled window moved the offset past its records`() {
        val after = Instant.parse("2024-01-31T00:00:00Z")

        val heartbeat = HuaweiSourceTask.heartbeat(
            "hb",
            partition,
            key,
            before,
            after,
            listOf(record(Instant.parse("2024-01-05T00:00:00Z"))),
        )

        assertEquals(mapOf("timestamp" to after.epochSecond), heartbeat?.sourceOffset())
    }
}
