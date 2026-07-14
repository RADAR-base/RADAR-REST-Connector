package org.radarbase.huawei.user

import java.io.IOException

/** User repository for Huawei Health Kit users. */
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
     * @throws UserNotAuthorizedException if the refresh token is no longer valid. Manual action
     * should be taken to get a new refresh token.
     * @throws NoSuchElementException if the user does not exist in this repository.
     */
    @Throws(IOException::class, UserNotAuthorizedException::class)
    fun getAccessToken(user: User): String
}
