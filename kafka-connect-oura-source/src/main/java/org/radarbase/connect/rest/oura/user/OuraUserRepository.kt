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

import org.radarbase.connect.rest.oura.OuraRestSourceConnectorConfig
import org.radarbase.oura.user.User
import org.radarbase.oura.user.UserNotAuthorizedException
import org.radarbase.oura.user.UserRepository
import org.slf4j.LoggerFactory
import java.io.IOException

@Suppress("unused")
abstract class OuraUserRepository : UserRepository {
    abstract fun initialize(config: OuraRestSourceConnectorConfig)

    @Throws(IOException::class, UserNotAuthorizedException::class)
    abstract fun refreshAccessToken(user: User): String

    @Throws(IOException::class)
    abstract fun applyPendingUpdates()

    abstract fun hasPendingUpdates(): Boolean

    companion object {
        private val logger = LoggerFactory.getLogger(OuraUserRepository::class.java)
    }
}
