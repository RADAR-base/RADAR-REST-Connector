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

package org.radarbase.huawei.route

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.radarbase.huawei.user.HuaweiUser
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserRepository
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Converts responses recorded from the live Huawei Health Kit API (`huawei-responses/<route
 * key>.json`, saved by `scripts/huawei-api-probe.sh` and trimmed by
 * `scripts/huawei-response-fixtures.py`) and compares every converted record with its expected
 * snapshot (`huawei-responses/expected/<route key>.json`), ignoring `timeReceived`.
 * A snapshot pins each Avro field to the value Huawei actually sent, so a mistyped field-value key
 * or JSON path shows up as a field turning `null`.
 *
 * @author yatharthranjan
 */
class HuaweiRecordedResponseTest {
    private val mapper = ObjectMapper()
    private val user: User = HuaweiUser(
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
    private val userRepository = object : UserRepository {
        override fun get(key: String): User = user
        override fun stream(): Sequence<User> = sequenceOf(user)
        override fun getAccessToken(user: User): String = "token"
    }

    @Test
    fun `recorded responses convert to their expected records`() {
        val responses = responseFiles(RESPONSES)
        assertTrue(responses.isNotEmpty(), "no recorded responses found")

        val mismatches = responses.mapNotNull { (key, file) ->
            val expectedFile = File(RESPONSES, "expected/$key.json")
            check(expectedFile.exists()) { "missing expected snapshot for $key" }
            val expected = mapper.readTree(expectedFile)
            val actual = mapper.createArrayNode().apply {
                convert(key, file).forEach { add(it) }
            }
            if (actual == expected) null else "$key:\n  expected $expected\n  actual   $actual"
        }

        assertTrue(mismatches.isEmpty(), "Mismatches:\n" + mismatches.joinToString("\n"))
    }

    @Test
    fun `recorded empty responses convert to no records`() {
        val responses = responseFiles(File(RESPONSES, "empty"))
        assertTrue(responses.isNotEmpty(), "no recorded empty responses found")

        responses.forEach { (key, file) ->
            assertEquals(emptyList(), convert(key, file), key)
        }
    }

    private fun convert(key: String, file: File): List<JsonNode> {
        val definition = HuaweiRouteFactory.definitions.single { it.key == key }
        val route = definition.build(userRepository, definition.defaultTopic)
        return route.converters.single()
            .processRecords(mapper.readTree(file), user)
            .map { result ->
                val record = result.getOrThrow()
                assertEquals(definition.defaultTopic, record.topic, key)
                (mapper.readTree(record.value.toString()) as ObjectNode).apply {
                    remove("timeReceived")
                }
            }
            .toList()
    }

    private fun responseFiles(dir: File): List<Pair<String, File>> =
        dir.listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .sortedBy { it.name }
            .map { it.nameWithoutExtension to it }

    companion object {
        private const val RESPONSES_DIR = "huawei-responses"
        private val RESPONSES = File(
            checkNotNull(HuaweiRecordedResponseTest::class.java.getResource("/$RESPONSES_DIR")) {
                "test resources $RESPONSES_DIR not found"
            }.toURI(),
        )
    }
}
