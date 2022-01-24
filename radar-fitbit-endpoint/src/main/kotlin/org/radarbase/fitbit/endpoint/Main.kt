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

package org.radarbase.fitbit.endpoint

import org.radarbase.fitbit.endpoint.config.FitbitEndpointConfig
import org.radarbase.jersey.GrizzlyServer
import org.radarbase.jersey.config.ConfigLoader
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    val logger = LoggerFactory.getLogger("org.radarbase.fitbit.endpoint.MainKt")

    val config = try {
        ConfigLoader.loadConfig<FitbitEndpointConfig>(
            listOf(
                "fitbit.yml",
                "/etc/radar-fitbit-endpoint/fitbit.yml"
            ),
            args,
        ).copyFromEnv()
    } catch (ex: IllegalArgumentException) {
        logger.error("No configuration file was found.")
        logger.error("Usage: radar-fitbit-endpoint <config-file>")
        exitProcess(1)
    }

    try {
        config.validate()
    } catch (ex: IllegalStateException) {
        logger.error("Configuration incomplete: {}", ex.message)
        exitProcess(1)
    }

    val resources = ConfigLoader.loadResources(config.resourceConfig, config)
    val server = GrizzlyServer(config.server.baseUri, resources, config.server.isJmxEnabled)
    server.listen()
}
