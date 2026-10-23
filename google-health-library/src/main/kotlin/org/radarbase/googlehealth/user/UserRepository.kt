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
package org.radarbase.googlehealth.user

import org.radarbase.googlehealth.exception.UserNotAuthorizedException
import java.io.IOException

/** User repository for users. */
interface UserRepository {
    @Throws(IOException::class)
    operator fun get(key: String): User?

    @Throws(IOException::class)
    fun stream(): Sequence<User>

    @Throws(IOException::class, UserNotAuthorizedException::class)
    fun getAccessToken(user: User): String

    @Throws(IOException::class, UserNotAuthorizedException::class)
    fun getRefreshToken(user: User): String

    @Throws(NoSuchElementException::class, IOException::class)
    fun findByExternalId(externalId: String): User = stream()
        .firstOrNull { it.serviceUserId == externalId }
        ?: throw NoSuchElementException("User not found in the User repository")

    fun hasPendingUpdates(): Boolean

    @Throws(IOException::class)
    fun applyPendingUpdates()
}
