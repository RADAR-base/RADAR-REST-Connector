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

package org.radarbase.connect.rest.huawei.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.radarbase.connect.rest.huawei.HuaweiRestSourceConnectorConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import kotlin.test.assertEquals

/**
 * @author yatharthranjan
 */
class HuaweiYamlUserRepositoryTest {
    @TempDir
    lateinit var dir: Path

    private fun writeUser(id: String, refreshToken: String, accessToken: String = "access") {
        Files.writeString(
            dir.resolve("$id.yml"),
            """
            id: $id
            projectId: p
            userId: $id
            sourceId: s
            startDate: 2024-01-01T00:00:00Z
            oauth2:
              accessToken: $accessToken
              refreshToken: $refreshToken
              expiresAt: 2999-01-01T00:00:00Z
            """.trimIndent(),
        )
    }

    private fun repository(users: String = ""): HuaweiYamlUserRepository {
        val config = HuaweiRestSourceConnectorConfig(
            mutableMapOf(
                "huawei.api.client" to "client",
                "huawei.api.secret" to "secret",
                "huawei.user.dir" to dir.toString(),
                "huawei.users" to users,
            ),
            false,
        )
        return HuaweiYamlUserRepository().apply { initialize(config) }
    }

    @Test
    fun `streams only users assigned to the task and skips unreadable files`() {
        writeUser("a", "refresh-a")
        writeUser("b", "refresh-b")
        writeUser("c", "")
        Files.writeString(dir.resolve("broken.yml"), "id: [unterminated")

        assertEquals(setOf("a", "b"), repository().stream().map { it.id }.toSet())
        assertEquals(setOf("b"), repository("b").stream().map { it.id }.toSet())
    }

    @Test
    fun `users are equal regardless of tokens and read time`() {
        writeUser("a", "refresh-a")
        val first = repository().stream().single()
        writeUser("a", "refresh-a2", accessToken = "other")
        val second = repository().stream().single()

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `invalidated access token is no longer returned as valid`() {
        writeUser("a", "refresh-a")
        val repository = repository()
        val user = repository.stream().single()
        assertEquals("access", repository.getAccessToken(user))

        repository.invalidateAccessToken(user)

        val credentials = (repository["a"] as HuaweiLocalUser).oauth2Credentials
        assertEquals(true, credentials.isAccessTokenExpired)
    }

    @Test
    fun `edited user files are reloaded`() {
        writeUser("a", "refresh-a", accessToken = "old")
        val repository = repository()
        assertEquals("old", repository.getAccessToken(repository.stream().single()))

        writeUser("a", "refresh-a", accessToken = "new")
        Files.setLastModifiedTime(
            dir.resolve("a.yml"),
            FileTime.from(Instant.now().plusSeconds(60)),
        )
        repository.applyPendingUpdates()

        assertEquals("new", repository.getAccessToken(repository.stream().single()))
    }
}
