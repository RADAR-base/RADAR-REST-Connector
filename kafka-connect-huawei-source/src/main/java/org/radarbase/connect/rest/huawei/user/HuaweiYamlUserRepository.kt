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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.radarbase.connect.rest.huawei.HuaweiRestSourceConnectorConfig
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserNotAuthorizedException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors

/**
 * User repository that reads (and writes refreshed tokens back to) YAML files in a local
 * directory, one file per user - mirrors Fitbit's `YamlUserRepository`. This is the easiest way
 * to run this connector locally without standing up a rest-source-authorizer webservice: register
 * a Huawei Health Kit OAuth2 app, obtain one user's access/refresh token by hand (e.g. via
 * Huawei's OAuth 2.0 authorization code flow), and drop them into a file under the directory
 * configured by `huawei.user.dir` - see `docker/huawei-user.yml.template`.
 *
 * @author yatharthranjan
 */
@Suppress("unused")
class HuaweiYamlUserRepository : HuaweiUserRepository() {
    private val client = OkHttpClient()
    private val users = ConcurrentHashMap<String, LockedUser>()
    private val nextFetch = AtomicReference(Instant.EPOCH)
    private lateinit var credentialsDir: Path
    private lateinit var clientCredentials: Headers

    override fun initialize(config: HuaweiRestSourceConnectorConfig) {
        credentialsDir = config.getHuaweiUserCredentialsPath()
        Files.createDirectories(credentialsDir)
        val credentialString = "${config.getHuaweiClient()}:${config.getHuaweiClientSecret()}"
        val credentialsBase64 = Base64.getEncoder().encodeToString(credentialString.toByteArray())
        clientCredentials = Headers.headersOf("Authorization", "Basic $credentialsBase64")
    }

    override operator fun get(key: String): User? {
        updateUsers()
        return users[key]?.locked { it.copy() }
    }

    override fun stream(): Sequence<User> {
        if (nextFetch.get() == Instant.EPOCH) {
            applyPendingUpdates()
        }
        return users.values.asSequence()
            .filter { it.locked { u -> u.oauth2Credentials.hasRefreshToken() } }
            .map { it.locked { u -> u.copy() } }
    }

    @Throws(IOException::class, UserNotAuthorizedException::class)
    override fun getAccessToken(user: User): String {
        updateUsers()
        val actual = users[user.id]
            ?: throw NoSuchElementException("User $user is not present in this user repository.")
        val current = actual.locked { u ->
            if (!u.oauth2Credentials.isAccessTokenExpired) u.oauth2Credentials.accessToken else null
        }
        return current ?: refreshAccessToken(user)
    }

    @Throws(IOException::class, UserNotAuthorizedException::class)
    override fun refreshAccessToken(user: User): String {
        val actual = users[user.id]
            ?: throw NoSuchElementException("User $user is not present in this user repository.")
        val refreshToken = actual.locked { it.oauth2Credentials.refreshToken }
        val node = requestAccessToken(refreshToken)

        val expiresIn = node["expires_in"]?.asLong()
        val accessToken = node["access_token"]?.asText()
            ?: throw UserNotAuthorizedException("Did not get an access token")
        val newRefreshToken = node["refresh_token"]?.asText() ?: refreshToken

        actual.update { u ->
            u.oauth2Credentials = OAuth2UserCredentials(newRefreshToken, accessToken, expiresIn)
            store(actual.path, u)
        }
        return accessToken
    }

    override fun hasPendingUpdates(): Boolean = Instant.now().isAfter(nextFetch.get())

    @Throws(IOException::class)
    override fun applyPendingUpdates() {
        forceUpdateUsers()
        nextFetch.set(Instant.now().plus(FETCH_THRESHOLD))
    }

    private fun updateUsers() {
        val next = nextFetch.get()
        val now = Instant.now()
        if (!now.isAfter(next) || !nextFetch.compareAndSet(next, now.plus(FETCH_THRESHOLD))) {
            return
        }
        forceUpdateUsers()
    }

    private fun forceUpdateUsers() {
        try {
            Files.walk(credentialsDir).use { walker ->
                val newUsers = walker
                    .filter {
                        Files.isRegularFile(it) &&
                            it.fileName.toString().lowercase().endsWith(".yml")
                    }
                    .map { path ->
                        LockedUser(
                            YAML_READER.readValue(path.toFile(), HuaweiLocalUser::class.java),
                            path,
                        )
                    }
                    .collect(Collectors.toMap({ it.locked { u -> u.id } }, { it }))
                users.keys.retainAll(newUsers.keys)
                newUsers.forEach { (id, u) -> users.putIfAbsent(id, u) }
            }
        } catch (ex: IOException) {
            logger.error("Failed to read user directory: {}", ex.toString())
        }
    }

    private fun requestAccessToken(refreshToken: String?): JsonNode {
        if (refreshToken.isNullOrEmpty()) {
            throw UserNotAuthorizedException("Refresh token is not set")
        }
        val request = Request.Builder()
            .url(HUAWEI_TOKEN_URL)
            .headers(clientCredentials)
            .post(
                FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build(),
            )
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            return when {
                response.isSuccessful && body != null -> JSON_READER.readTree(body)
                response.code == 400 || response.code == 401 ->
                    throw UserNotAuthorizedException("Refresh token is no longer valid.")
                else -> throw IOException(
                    "Failed to request refresh token, HTTP status ${response.code}" +
                        (body?.let { " and content $it" } ?: ""),
                )
            }
        }
    }

    private fun store(path: Path, user: HuaweiLocalUser) {
        try {
            val temp = Files.createTempFile(user.id, ".tmp")
            try {
                Files.newOutputStream(temp).use { out -> YAML_WRITER.writeValue(out, user) }
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(temp)
            }
        } catch (ex: IOException) {
            logger.error("Failed to store user file: {}", ex.toString())
        }
    }

    /** Guards a mutable [HuaweiLocalUser] against concurrent read/refresh/store. */
    private class LockedUser(val user: HuaweiLocalUser, val path: Path) {
        private val lock = ReentrantLock()

        fun <V> locked(block: (HuaweiLocalUser) -> V): V {
            lock.lock()
            try {
                return block(user)
            } finally {
                lock.unlock()
            }
        }

        fun update(block: (HuaweiLocalUser) -> Unit) {
            lock.lock()
            try {
                block(user)
            } finally {
                lock.unlock()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HuaweiYamlUserRepository::class.java)
        private const val HUAWEI_TOKEN_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/token"
        private val FETCH_THRESHOLD = Duration.ofHours(1L)
        private val YAML_MAPPER = ObjectMapper(YAMLFactory()).apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
        private val YAML_READER = YAML_MAPPER.reader()
        private val YAML_WRITER = YAML_MAPPER.writerFor(HuaweiLocalUser::class.java)
        private val JSON_READER = ObjectMapper().registerModule(JavaTimeModule()).reader()
    }
}
