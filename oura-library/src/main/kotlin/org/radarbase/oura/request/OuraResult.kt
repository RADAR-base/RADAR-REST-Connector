package org.radarbase.oura.request

sealed class OuraResult<out T : Any> {
    data class Success<out T : Any>(val value: T) : OuraResult<T>()
    data class Error(val error: OuraError) : OuraResult<Nothing>()
}

sealed interface OuraError

sealed class OuraErrorBase(val message: String, val cause: Exception? = null) : OuraError

class OuraRateLimitError : OuraErrorBase("Rate limit reached..", TooManyRequestsException())
