/*
 * Copyright 2026 King's College London
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.googlehealth.user

import java.io.IOException

abstract class GoogleHealthUserRepository : UserRepository {

    @Throws(NoSuchElementException::class, IOException::class)
    abstract override fun findByExternalId(externalId: String): User

    @Throws(IOException::class)
    abstract fun getOAuth2AccessToken(user: User): String

    abstract fun deregisterUser(serviceUserId: String)

    abstract fun fetchUnauthorizedUsers(): List<User>
}
