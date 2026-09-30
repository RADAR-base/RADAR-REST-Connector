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

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.apache.kafka.common.config.AbstractConfig
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.Importance
import org.apache.kafka.common.config.ConfigDef.NonEmptyString
import org.apache.kafka.common.config.ConfigDef.Type
import org.apache.kafka.common.config.ConfigDef.Width
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.connect.errors.ConnectException
import org.radarbase.connect.rest.huawei.user.HuaweiServiceUserRepository
import org.radarbase.connect.rest.huawei.user.HuaweiUserRepository
import org.radarbase.huawei.route.HuaweiRouteFactory
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

/**
 * Kafka Connect configuration for the Huawei Health Kit source connector.
 *
 * Every data type registered in [HuaweiRouteFactory.definitions] gets a `huawei.<key>.enabled`
 * boolean and a `huawei.<key>.topic` string config, generated from that single shared registry
 * instead of ~110 hand-duplicated `ConfigDef` entries (one connector, one config, one canonical
 * list of Huawei data types).
 *
 * @author yatharthranjan
 */
class HuaweiRestSourceConnectorConfig(
    config: ConfigDef,
    parsedConfig: MutableMap<String, String>,
    doLog: Boolean,
) : AbstractConfig(config, parsedConfig, doLog) {

    constructor(parsedConfig: MutableMap<String, String>, doLog: Boolean) : this(
        conf(),
        parsedConfig,
        doLog,
    )

    private var userRepository: HuaweiUserRepository? = null

    fun getHuaweiUsers(): List<String> = getList(HUAWEI_USERS_CONFIG)

    fun getHuaweiClient(): String = getString(HUAWEI_API_CLIENT_CONFIG)

    fun getHuaweiClientSecret(): String = getPassword(HUAWEI_API_SECRET_CONFIG).value()

    fun getUserRepository(reuse: HuaweiUserRepository?): HuaweiUserRepository {
        val configuredClass = getClass(HUAWEI_USER_REPOSITORY_CONFIG)
        val repo = if (reuse != null && reuse.javaClass == configuredClass) {
            reuse
        } else {
            createUserRepository()
        }
        repo.initialize(this)
        userRepository = repo
        return repo
    }

    fun getUserRepository(): HuaweiUserRepository {
        val repo = checkNotNull(userRepository) { "User repository has not been initialized" }
        repo.initialize(this)
        return repo
    }

    @Suppress("UNCHECKED_CAST")
    private fun createUserRepository(): HuaweiUserRepository = try {
        (getClass(HUAWEI_USER_REPOSITORY_CONFIG) as Class<out HuaweiUserRepository>)
            .getDeclaredConstructor()
            .newInstance()
    } catch (e: ReflectiveOperationException) {
        throw ConnectException("Invalid class. $e")
    }

    /**
     * Directory containing per-user YAML credential files, for the file-based
     * [org.radarbase.connect.rest.huawei.user.HuaweiYamlUserRepository]. Only used if that
     * repository is configured via [HUAWEI_USER_REPOSITORY_CONFIG].
     */
    fun getHuaweiUserCredentialsPath(): Path =
        Paths.get(getString(HUAWEI_USER_CREDENTIALS_DIR_CONFIG))

    fun getHuaweiUserRepositoryUrl(): HttpUrl {
        var urlString = getString(HUAWEI_USER_REPOSITORY_URL_CONFIG).trim()
        if (urlString.isNotEmpty() && urlString.last() != '/') {
            urlString += "/"
        }
        return urlString.toHttpUrlOrNull()
            ?: throw ConfigException(
                HUAWEI_USER_REPOSITORY_URL_CONFIG,
                urlString,
                "User repository URL $urlString cannot be parsed as URL.",
            )
    }

    fun getPollIntervalPerUser(): Duration = Duration.ofSeconds(
        getInt(HUAWEI_USER_POLL_INTERVAL_CONFIG).toLong(),
    )

    fun getHuaweiUserRepositoryClientId(): String = getString(
        HUAWEI_USER_REPOSITORY_CLIENT_ID_CONFIG,
    )

    fun getHuaweiUserRepositoryClientSecret(): String =
        getPassword(HUAWEI_USER_REPOSITORY_CLIENT_SECRET_CONFIG).value()

    fun getHuaweiUserRepositoryTokenUrl(): URL? {
        val value = getString(HUAWEI_USER_REPOSITORY_TOKEN_URL_CONFIG)
        if (value.isNullOrEmpty()) {
            return null
        }
        return try {
            URL(value)
        } catch (e: MalformedURLException) {
            throw ConfigException("Huawei user repository token URL is invalid.")
        }
    }

    /**
     * The (config key -> Kafka topic) pairs of every Huawei data type that is enabled in this
     * configuration.
     */
    fun enabledTopics(): Map<String, String> =
        HuaweiRouteFactory.definitions
            .filter { getBoolean(enabledKey(it.key)) }
            .associate { it.key to getString(topicKey(it.key)) }

    companion object {
        private const val SOURCE_POLL_INTERVAL_CONFIG = "rest.source.poll.interval.ms"
        private const val SOURCE_POLL_INTERVAL_DOC = "How often to poll the source URL."
        private const val SOURCE_POLL_INTERVAL_DISPLAY = "Polling interval"
        private const val SOURCE_POLL_INTERVAL_DEFAULT = 60000L

        const val SOURCE_URL_CONFIG = "rest.source.base.url"
        private const val SOURCE_URL_DOC = "Base URL for REST source connector."
        private const val SOURCE_URL_DISPLAY = "Base URL for REST source connector."
        const val SOURCE_URL_DEFAULT = "https://health-api.cloud.huawei.com/healthkit/v1"

        const val HUAWEI_USERS_CONFIG = "huawei.users"
        private const val HUAWEI_USERS_DOC =
            "The user ID of Huawei users to include in polling, separated by commas. " +
                "Non existing user names will be ignored. " +
                "If empty, all users in the user directory will be used."
        private const val HUAWEI_USERS_DISPLAY = "Huawei users"

        const val HUAWEI_API_CLIENT_CONFIG = "huawei.api.client"
        private const val HUAWEI_API_CLIENT_DOC = "Client ID for the Huawei Health Kit API"
        private const val HUAWEI_API_CLIENT_DISPLAY = "Huawei API client ID"

        const val HUAWEI_API_SECRET_CONFIG = "huawei.api.secret"
        private const val HUAWEI_API_SECRET_DOC =
            "Secret for the Huawei API client set in huawei.api.client."
        private const val HUAWEI_API_SECRET_DISPLAY = "Huawei API client secret"

        const val HUAWEI_USER_REPOSITORY_CONFIG = "huawei.user.repository.class"
        private const val HUAWEI_USER_REPOSITORY_DOC =
            "Class for managing users and authentication."
        private const val HUAWEI_USER_REPOSITORY_DISPLAY = "User repository class"

        const val HUAWEI_USER_POLL_INTERVAL_CONFIG = "huawei.user.poll.interval"
        private const val HUAWEI_USER_POLL_INTERVAL_DOC =
            "Polling interval per Huawei user per request route in seconds."
        private const val HUAWEI_USER_POLL_INTERVAL_DEFAULT = 150
        private const val HUAWEI_USER_POLL_INTERVAL_DISPLAY = "Per-user per-route polling interval."

        const val HUAWEI_USER_CREDENTIALS_DIR_CONFIG = "huawei.user.dir"
        private const val HUAWEI_USER_CREDENTIALS_DIR_DOC =
            "Directory containing Huawei user information and credentials. Only used if a " +
                "file-based user repository is configured."
        private const val HUAWEI_USER_CREDENTIALS_DIR_DISPLAY = "User directory"
        private const val HUAWEI_USER_CREDENTIALS_DIR_DEFAULT =
            "/var/lib/kafka-connect-huawei-source/users"

        const val HUAWEI_USER_REPOSITORY_URL_CONFIG = "huawei.user.repository.url"
        private const val HUAWEI_USER_REPOSITORY_URL_DOC =
            "URL for webservice containing user credentials. Only used if a webservice-based " +
                "user repository is configured."
        private const val HUAWEI_USER_REPOSITORY_URL_DISPLAY = "User repository URL"
        private const val HUAWEI_USER_REPOSITORY_URL_DEFAULT = ""

        const val HUAWEI_USER_REPOSITORY_CLIENT_ID_CONFIG = "huawei.user.repository.client.id"
        private const val HUAWEI_USER_REPOSITORY_CLIENT_ID_DOC =
            "Client ID for connecting to the service repository."
        private const val HUAWEI_USER_REPOSITORY_CLIENT_ID_DISPLAY =
            "Client ID for user repository."

        const val HUAWEI_USER_REPOSITORY_CLIENT_SECRET_CONFIG =
            "huawei.user.repository.client.secret"
        private const val HUAWEI_USER_REPOSITORY_CLIENT_SECRET_DOC =
            "Client secret for connecting to the service repository."
        private const val HUAWEI_USER_REPOSITORY_CLIENT_SECRET_DISPLAY =
            "Client Secret for user repository."

        const val HUAWEI_USER_REPOSITORY_TOKEN_URL_CONFIG =
            "huawei.user.repository.oauth2.token.url"
        private const val HUAWEI_USER_REPOSITORY_TOKEN_URL_DOC =
            "OAuth 2.0 token url for retrieving client credentials."
        private const val HUAWEI_USER_REPOSITORY_TOKEN_URL_DISPLAY = "OAuth 2.0 token URL."

        private fun enabledKey(key: String) = "huawei.$key.enabled"
        private fun topicKey(key: String) = "huawei.$key.topic"

        @JvmStatic
        fun conf(): ConfigDef {
            val group = "Huawei"
            var order = 0

            val def = ConfigDef()
                .define(
                    SOURCE_POLL_INTERVAL_CONFIG,
                    Type.LONG,
                    SOURCE_POLL_INTERVAL_DEFAULT,
                    Importance.LOW,
                    SOURCE_POLL_INTERVAL_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    SOURCE_POLL_INTERVAL_DISPLAY,
                )
                .define(
                    SOURCE_URL_CONFIG,
                    Type.STRING,
                    SOURCE_URL_DEFAULT,
                    Importance.HIGH,
                    SOURCE_URL_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    SOURCE_URL_DISPLAY,
                )
                .define(
                    HUAWEI_USERS_CONFIG,
                    Type.LIST,
                    emptyList<String>(),
                    Importance.HIGH,
                    HUAWEI_USERS_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_USERS_DISPLAY,
                )
                .define(
                    HUAWEI_API_CLIENT_CONFIG,
                    Type.STRING,
                    ConfigDef.NO_DEFAULT_VALUE,
                    NonEmptyString(),
                    Importance.HIGH,
                    HUAWEI_API_CLIENT_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_API_CLIENT_DISPLAY,
                )
                .define(
                    HUAWEI_API_SECRET_CONFIG,
                    Type.PASSWORD,
                    ConfigDef.NO_DEFAULT_VALUE,
                    Importance.HIGH,
                    HUAWEI_API_SECRET_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_API_SECRET_DISPLAY,
                )
                .define(
                    HUAWEI_USER_POLL_INTERVAL_CONFIG,
                    Type.INT,
                    HUAWEI_USER_POLL_INTERVAL_DEFAULT,
                    Importance.MEDIUM,
                    HUAWEI_USER_POLL_INTERVAL_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_USER_POLL_INTERVAL_DISPLAY,
                )
                .define(
                    HUAWEI_USER_REPOSITORY_CONFIG,
                    Type.CLASS,
                    HuaweiServiceUserRepository::class.java,
                    Importance.MEDIUM,
                    HUAWEI_USER_REPOSITORY_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_USER_REPOSITORY_DISPLAY,
                )
                .define(
                    HUAWEI_USER_CREDENTIALS_DIR_CONFIG,
                    Type.STRING,
                    HUAWEI_USER_CREDENTIALS_DIR_DEFAULT,
                    Importance.LOW,
                    HUAWEI_USER_CREDENTIALS_DIR_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_USER_CREDENTIALS_DIR_DISPLAY,
                )
                .define(
                    HUAWEI_USER_REPOSITORY_URL_CONFIG,
                    Type.STRING,
                    HUAWEI_USER_REPOSITORY_URL_DEFAULT,
                    Importance.LOW,
                    HUAWEI_USER_REPOSITORY_URL_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_USER_REPOSITORY_URL_DISPLAY,
                )
                .define(
                    HUAWEI_USER_REPOSITORY_CLIENT_ID_CONFIG,
                    Type.STRING,
                    "",
                    Importance.MEDIUM,
                    HUAWEI_USER_REPOSITORY_CLIENT_ID_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_USER_REPOSITORY_CLIENT_ID_DISPLAY,
                )
                .define(
                    HUAWEI_USER_REPOSITORY_CLIENT_SECRET_CONFIG,
                    Type.PASSWORD,
                    "",
                    Importance.MEDIUM,
                    HUAWEI_USER_REPOSITORY_CLIENT_SECRET_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_USER_REPOSITORY_CLIENT_SECRET_DISPLAY,
                )
                .define(
                    HUAWEI_USER_REPOSITORY_TOKEN_URL_CONFIG,
                    Type.STRING,
                    "",
                    Importance.MEDIUM,
                    HUAWEI_USER_REPOSITORY_TOKEN_URL_DOC,
                    group,
                    ++order,
                    Width.SHORT,
                    HUAWEI_USER_REPOSITORY_TOKEN_URL_DISPLAY,
                )

            HuaweiRouteFactory.definitions.forEach { d ->
                val label = d.key.replace('_', ' ')
                def.define(
                    enabledKey(d.key), Type.BOOLEAN, d.enabledByDefault, Importance.LOW,
                    "Enable or disable Huawei $label", group, ++order, Width.SHORT,
                    "Huawei $label enabled",
                )
                def.define(
                    topicKey(d.key), Type.STRING, d.defaultTopic, Importance.LOW,
                    "Kafka topic for Huawei $label", group, ++order, Width.SHORT,
                    "Huawei $label topic",
                )
            }

            return def
        }
    }
}
