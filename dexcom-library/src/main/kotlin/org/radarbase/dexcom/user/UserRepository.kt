package org.radarbase.dexcom.user

import java.io.IOException

interface UserRepository {
    @Throws(IOException::class)
    operator fun get(key: String): User?

    @Throws(IOException::class)
    fun stream(): Sequence<User>

    @Throws(IOException::class)
    fun getAccessToken(user: User): String
}
