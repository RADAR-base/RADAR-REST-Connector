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

import org.radarbase.jersey.config.ConfigLoader.copyEnv

data class FitbitConfig(
    val verificationCode: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
) {
    fun withEnv(): FitbitConfig = this
        .copyEnv("FITBIT_CLIENT_ID") { copy(clientId = it) }
        .copyEnv("FITBIT_CLIENT_SECRET") { copy(clientSecret = it) }
        .copyEnv("FITBIT_VERIFICATION_CODE") { copy(verificationCode = it) }

    fun validate() {
        requireNotNull(verificationCode) { "fitbit.verificationCode configuration or FITBIT_VERIFICATION_CODE environment variable must be set." }
        requireNotNull(clientId) { "fitbit.clientId configuration or FITBIT_CLIENT_ID environment variable must be set." }
        requireNotNull(clientSecret) { "fitbit.clientSecret configuration or FITBIT_CLIENT_SECRET environment variable must be set." }
    }
}
