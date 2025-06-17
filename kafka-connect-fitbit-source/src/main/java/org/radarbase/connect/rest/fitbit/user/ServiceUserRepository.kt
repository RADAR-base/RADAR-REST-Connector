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
package org.radarbase.connect.rest.fitbit.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
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
import org.radarbase.connect.rest.RestSourceConnectorConfig
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig
import org.radarbase.kotlin.coroutines.CacheConfig
import org.radarbase.kotlin.coroutines.CachedSet
import org.radarbase.kotlin.coroutines.CachedValue
import org.radarbase.ktor.auth.ClientCredentialsConfig
import org.radarbase.ktor.auth.clientCredentials
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

@Suppress("unused")
class ServiceUserRepository : UserRepository {
    private lateinit var userCache: CachedSet<User>
    private lateinit var client: HttpClient
    private val credentialCaches = ConcurrentHashMap<String, CachedValue<OAuth2UserCredentials>>()
    private val credentialCacheConfig =
        CacheConfig(refreshDuration = 1.days, retryDuration = 1.minutes)
    private val mapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    
    // User repository service token cache for OAuth2 client credentials authentication
    private var userRepositoryTokenCache: CachedValue<String>? = null
    private val userRepositoryCacheConfig = CacheConfig(
        refreshDuration = 50.minutes,
        retryDuration = 1.minutes,
    )

    @Throws(IOException::class)
    override fun get(key: String): User = runBlocking(Dispatchers.Default) {
        makeRequest { url("users/$key") }
    }

    override fun initialize(
        config: RestSourceConnectorConfig,
    ) {
        config as FitbitRestSourceConnectorConfig
        val containedUsers = config.fitbitUsers.toHashSet()

        client = createClient(
            baseUrl = URLBuilder(config.fitbitUserRepositoryUrl.toString()).build(),
            tokenUrl = URLBuilder(config.fitbitUserRepositoryTokenUrl.toString()).build(),
            clientId = config.fitbitUserRepositoryClientId,
            clientSecret = config.fitbitUserRepositoryClientSecret,
        )

        if (config.fitbitUserRepositoryTokenUrl.toString().isNotBlank()) {
            userRepositoryTokenCache = CachedValue(userRepositoryCacheConfig) {
                requestUserRepositoryToken(
                    tokenUrl = URLBuilder(config.fitbitUserRepositoryTokenUrl.toString()).build(),
                    clientId = config.fitbitUserRepositoryClientId,
                    clientSecret = config.fitbitUserRepositoryClientSecret,
                    scope = "SUBJECT.READ MEASUREMENT.CREATE",
                    audience = "res_restAuthorizer",
                )
            }
        }

        val refreshDuration = config.userCacheRefreshInterval.toKotlinDuration()
        userCache = CachedSet(
            CacheConfig(
                refreshDuration = refreshDuration,
                retryDuration = if (refreshDuration > 1.minutes) 1.minutes else refreshDuration,
            ),
        ) {
            makeRequest<Users> { url("users?source-type=FitBit") }
                .users
                .filterTo(HashSet()) { u: User ->
                    u.isComplete &&
                        (containedUsers.isEmpty() || u.versionedId in containedUsers)
                }
        }
    }

    private fun createClient(
        baseUrl: Url,
        tokenUrl: Url?,
        clientId: String?,
        clientSecret: String?,
    ): HttpClient = HttpClient(CIO) {
        // Only add basic auth if no token URL is provided (fallback authentication)
        if (tokenUrl == null && clientId != null && clientSecret != null) {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = clientId, password = clientSecret)
                    }
                    realm = "Access to the '/' path"
                    sendWithoutRequest {
                        it.url.host == baseUrl.host
                    }
                }
            }
        }

        defaultRequest {
            url.takeFrom(baseUrl)
        }

        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule()) // support java.time.* types
            }
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 60.seconds.inWholeMilliseconds
            requestTimeoutMillis = 90.seconds.inWholeMilliseconds
        }
    }

    private suspend fun requestUserRepositoryToken(
        tokenUrl: Url,
        clientId: String?,
        clientSecret: String?,
        scope: String?,
        audience: String?,
    ): String {
        return try {
            val authClient = HttpClient(CIO) {
                install(Auth) {
                    clientCredentials(
                        ClientCredentialsConfig(
                            tokenUrl.toString(),
                            clientId,
                            clientSecret,
                            scope,
                            audience,
                        ).copyWithEnv("MANAGEMENT_PORTAL"),
                        tokenUrl.host,
                    )
                }
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        },
                    )
                }
                install(HttpTimeout) {
                    connectTimeoutMillis = 60.seconds.inWholeMilliseconds
                    requestTimeoutMillis = 90.seconds.inWholeMilliseconds
                }
            }

            val response = authClient.request {
                url(tokenUrl)
                method = HttpMethod.Post
                setBody("grant_type=client_credentials&scope=${scope ?: ""}&audience=${audience ?: ""}")
                contentType(ContentType.Application.FormUrlEncoded)
            }

            authClient.close()

            if (!response.status.isSuccess()) {
                throw HttpResponseException(
                    "Failed to get user repository token: ${response.status}",
                    response.status.value,
                )
            }

            val tokenResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            tokenResponse["access_token"]?.toString()?.removeSurrounding("\"")
                ?: throw IllegalStateException("No access token in response")
        } catch (e: Exception) {
            logger.error("Failed to get user repository token", e)
            throw e
        }
    }

    private suspend fun getAuthorizationHeader(): String {
        return try {
            userRepositoryTokenCache?.get() ?: throw IllegalStateException("User repository token cache not initialized")
        } catch (e: Exception) {
            logger.error("Failed to get authorization header", e)
            throw e
        }
    }

    override fun stream(): Stream<out User> = runBlocking(Dispatchers.Default) {
        val valueInCache = userCache.getFromCache()
            .takeIf { it is CachedValue.CacheValue }
            ?.getOrThrow()

        (valueInCache ?: userCache.get())
            .stream()
            .filter { it.isComplete }
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
            val token = requestAccessToken(user) {
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
            if (ex.statusCode == 407) {
                credentialCaches -= user.id
                if (user is LocalUser) {
                    user.setIsAuthorized(false)
                }
                throw UserNotAuthorizedException(ex.message)
            }
            throw ex
        }

    override fun hasPendingUpdates(): Boolean = runBlocking(Dispatchers.Default) {
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
    ): T = withContext(Dispatchers.IO) {
        val response = client.request {
            builder()
            userRepositoryTokenCache?.let { tokenCache ->
                header("Authorization", "Bearer ${getAuthorizationHeader()}")
            }
        }
        val contentLength = response.contentLength()
        // if Transfer-Encoding: chunked, then the request has data but contentLength will be null.
        val transferEncoding = response.headers["Transfer-Encoding"]
        val hasBody = (contentLength != null && contentLength > 0) ||
            (transferEncoding != null && transferEncoding.contains("chunked"))
        if (response.status == HttpStatusCode.NotFound) {
            throw NoSuchElementException("URL " + response.request.url + " does not exist")
        } else if (!response.status.isSuccess() || !hasBody) {
            val message = buildString {
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

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceUserRepository::class.java)
    }
}
