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

package org.radarbase.fitbit.endpoint.config

import org.radarbase.fitbit.endpoint.inject.ManagementPortalEnhancerFactory
import org.radarbase.jersey.auth.AuthConfig
import org.radarbase.jersey.config.ConfigLoader.copyOnChange
import org.radarbase.jersey.enhancer.EnhancerFactory

data class FitbitEndpointConfig(
    /** Radar-jersey resource configuration class. */
    val resourceConfig: Class<out EnhancerFactory> = ManagementPortalEnhancerFactory::class.java,
    /** Server configurations. */
    val server: ServerConfig = ServerConfig(),
    /** Fitbit configuration. */
    val fitbit: FitbitConfig = FitbitConfig(),
    val auth: AuthConfig = AuthConfig(jwtResourceName = "res_fitbitEndpoint"),
) {
    fun withEnv(): FitbitEndpointConfig = this
        .copyOnChange(auth, { it.withEnv() }) { copy(auth = it) }
        .copyOnChange(fitbit, { it.withEnv() }) { copy(fitbit = it) }

    fun validate() {
        fitbit.validate()
    }
}

