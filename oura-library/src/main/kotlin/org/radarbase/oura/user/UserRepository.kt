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
package org.radarbase.oura.user

import java.io.IOException

/** User repository for users. */
interface UserRepository {
    /**
     * Get specified user.
     *
     * @throws IOException if the user cannot be retrieved from the repository.
     */
    @Throws(IOException::class)
    operator fun get(key: String): User?

    /**
     * Get all relevant users.
     *
     * @throws IOException if the list cannot be retrieved from the repository.
     */
    @Throws(IOException::class)
    fun stream(): Sequence<User>

    /**
     * Get the current access token of given user.
     *
     * @throws IOException if the new access token cannot be retrieved from the repository.
     * @throws NotAuthorizedException if the refresh token is no longer valid. Manual action should
     * be taken to get a new refresh token.
     * @throws NoSuchElementException if the user does not exists in this repository.
     */
    @Throws(IOException::class, UserNotAuthorizedException::class)
    fun getAccessToken(user: User): String

}
