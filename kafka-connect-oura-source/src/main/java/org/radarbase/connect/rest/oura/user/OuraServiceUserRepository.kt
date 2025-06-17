/*
 * Copyright 2018 The Hyve
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
package org.radarbase.connect.rest.oura.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.jackson.jackson
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.radarbase.connect.rest.oura.OuraRestSourceConnectorConfig
import org.radarbase.kotlin.coroutines.CacheConfig
import org.radarbase.kotlin.coroutines.CachedSet
import org.radarbase.kotlin.coroutines.CachedValue
import org.radarbase.oura.user.OuraUser
import org.radarbase.oura.user.User
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.streams.asSequence
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
class OuraServiceUserRepository : OuraUserRepository() {
    private lateinit var userCache: CachedSet<OuraUser>
    private lateinit var client: HttpClient
    private var userRepositoryTokenCache: CachedValue<String>? = null
    private var basicAuthCredentials: String? = null
    private val credentialCaches = ConcurrentHashMap<String, CachedValue<OAuth2UserCredentials>>()
    private val credentialCacheConfig =
        CacheConfig(
            refreshDuration = 1.days,
            retryDuration = 1.minutes,
        )
    private val userRepositoryCacheConfig =
        CacheConfig(
            refreshDuration = 50.minutes,
            retryDuration = 1.minutes,
        ) // Refresh before 1-hour expiry
    private val mapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    private var tokenUrl: String? = null
    private var clientId: String? = null
    private var clientSecret: String? = null

    @Throws(IOException::class)
    override fun get(key: String): User =
        runBlocking(Dispatchers.Default) {
            makeRequest { url("users/$key") }
        }

    override fun initialize(config: OuraRestSourceConnectorConfig) {
        val containedUsers = config.ouraUsers.toHashSet()

        val baseUrl = URLBuilder(config.ouraUserRepositoryUrl.toString()).build()
        tokenUrl = config.ouraUserRepositoryTokenUrl?.toString()
        clientId = config.ouraUserRepositoryClientId
        clientSecret = config.ouraUserRepositoryClientSecret

        if (tokenUrl != null && clientId != null && clientSecret != null) {
            logger.info(
                "Using OAuth2 client credentials authentication with token URL: $tokenUrl",
            )

            userRepositoryTokenCache = CachedValue(userRepositoryCacheConfig) {
                requestUserRepositoryToken()
            }
        } else if (!clientId.isNullOrBlank() && !clientSecret.isNullOrBlank()) {
            logger.info("Using basic authentication")
            basicAuthCredentials = "Basic " + java.util.Base64.getEncoder()
                .encodeToString("$clientId:$clientSecret".toByteArray())
        } else {
            logger.warn("No authentication configured - this may cause issues")
        }

        client = createClient(baseUrl)

        userCache =
            CachedSet(
                CacheConfig(refreshDuration = 1.hours, retryDuration = 1.minutes),
            ) {
                makeRequest<OuraUsers> { url("users?source-type=Oura") }
                    .users
                    .toHashSet()
                    .filterTo(HashSet()) { u ->
                        u.isComplete() &&
                            (containedUsers.isEmpty() || u.versionedId in containedUsers)
                    }
            }
    }

    private fun createClient(baseUrl: Url): HttpClient =
        HttpClient(CIO) {
            defaultRequest {
                url.takeFrom(baseUrl)
            }

            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                }
            }

            install(HttpTimeout) {
                connectTimeoutMillis = 60.seconds.inWholeMilliseconds
                requestTimeoutMillis = 90.seconds.inWholeMilliseconds
            }
        }

    private suspend fun requestUserRepositoryToken(): String {
        val tmpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        try {
            val response = tmpClient.request {
                method = HttpMethod.Post
                url(tokenUrl!!)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    "grant_type=client_credentials&scope=SUBJECT.READ MEASUREMENT.CREATE" +
                        "&audience=res_restAuthorizer",
                )
                headers.append(
                    "Authorization",
                    "Basic " + java.util.Base64.getEncoder()
                        .encodeToString("$clientId:$clientSecret".toByteArray()),
                )
            }

            if (response.status.isSuccess()) {
                val tokenData = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val accessToken = tokenData["access_token"]?.toString()?.trim('"')
                    ?: throw IOException("No access token in response")
                logger.debug("Successfully obtained user repository access token")
                return "Bearer $accessToken"
            } else {
                throw IOException(
                    "Failed to get repository token: ${response.status} - ${response.bodyAsText()}",
                )
            }
        } finally {
            tmpClient.close()
        }
    }

    override fun stream(): Sequence<OuraUser> =
        runBlocking(Dispatchers.Default) {
            val valueInCache =
                userCache.getFromCache()
                    .takeIf { it is CachedValue.CacheValue }
                    ?.getOrThrow()

            (valueInCache ?: userCache.get())
                .stream()
                .filter { it.isComplete() }
                .asSequence()
        }

    @Throws(IOException::class, UserNotAuthorizedException::class)
    override fun getAccessToken(user: User): String {
        if (!user.isAuthorized) {
            throw UserNotAuthorizedException("User is not authorized")
        }
        return runBlocking(Dispatchers.Default) {
            credentialCache(user)
                .get { !it.isAccessTokenExpired }
                .value
                .accessToken
        }
    }

    @Throws(IOException::class, UserNotAuthorizedException::class)
    override fun refreshAccessToken(user: User): String {
        if (!user.isAuthorized) {
            throw UserNotAuthorizedException("User is not authorized")
        }
        return runBlocking(Dispatchers.Default) {
            val token =
                requestAccessToken(user) {
                    url("users/${user.id}/token")
                    method = HttpMethod.Post
                    setBody("{}")
                    contentType(ContentType.Application.Json)
                }
            credentialCache(user).set(token)
            token.accessToken
        }
    }

    private suspend fun credentialCache(user: User): CachedValue<OAuth2UserCredentials> =
        credentialCaches.computeIfAbsent(user.id) {
            CachedValue(credentialCacheConfig) {
                requestAccessToken(user) { url("users/${user.id}/token") }
            }
        }

    @Throws(UserNotAuthorizedException::class, IOException::class)
    private suspend fun requestAccessToken(
        user: User,
        builder: HttpRequestBuilder.() -> Unit,
    ): OAuth2UserCredentials =
        try {
            makeRequest<OAuth2UserCredentials>(builder)
        } catch (ex: HttpResponseException) {
            when (ex.statusCode) {
                401 -> {
                    credentialCaches -= user.id
                    throw UserNotAuthorizedException(
                        "Token refresh failed - user needs re-authorization: ${ex.message}",
                    )
                }
                407 -> {
                    credentialCaches -= user.id
                    throw UserNotAuthorizedException("Proxy authentication required: ${ex.message}")
                }
                else -> throw ex
            }
        }

    override fun hasPendingUpdates(): Boolean =
        runBlocking(Dispatchers.Default) {
            userCache.isStale()
        }

    @Throws(IOException::class)
    override fun applyPendingUpdates() {
        logger.info("Requesting user information from webservice")

        runBlocking(Dispatchers.Default) {
            userCache.get()
        }
    }

    private suspend inline fun <reified T> makeRequest(
        crossinline builder: HttpRequestBuilder.() -> Unit,
    ): T =
        withContext(Dispatchers.IO) {
            val authorization = getAuthorizationHeader()

            val response = client.request {
                builder()
                if (authorization != null) {
                    headers.append("Authorization", authorization)
                }
            }

            val contentLength = response.contentLength()
            val hasBody = contentLength != null && contentLength > 0

            if (response.status == HttpStatusCode.NotFound) {
                throw NoSuchElementException("URL " + response.request.url + " does not exist")
            } else if (!response.status.isSuccess() || !hasBody) {
                val message =
                    buildString {
                        append("Failed to make request (HTTP status code ")
                        append(response.status)
                        append(')')
                        if (hasBody) {
                            append(": ")
                            append(response.bodyAsText())
                        }
                    }
                throw HttpResponseException(message, response.status.value)
            }
            mapper.readValue<T>(response.bodyAsText())
        }

    private suspend fun getAuthorizationHeader(): String? {
        return when {
            userRepositoryTokenCache != null -> {
                try {
                    userRepositoryTokenCache!!.get()
                } catch (ex: Exception) {
                    logger.error("Error getting user repository access token", ex)
                    null
                }
            }
            basicAuthCredentials != null -> basicAuthCredentials
            else -> null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OuraServiceUserRepository::class.java)
    }
}
