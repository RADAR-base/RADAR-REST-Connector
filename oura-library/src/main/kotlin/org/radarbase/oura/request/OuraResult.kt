package org.radarbase.oura.request

sealed class OuraResult<out T : Any> {
    data class Success<out T : Any>(val value: T) : OuraResult<T>()
    data class Error(val error: OuraError) : OuraResult<Nothing>()
}

sealed interface OuraError

sealed class OuraErrorBase(val message: String, val cause: Exception? = null) : OuraError

class OuraRateLimitError : OuraErrorBase("Rate limit reached..", TooManyRequestsException())

class OuraClientException : OuraErrorBase(
    "Client unsupported or unauthorized..",
    TooManyRequestsException(),
)

class OuraUnauthorizedAccessError : OuraErrorBase(
    "Access token expired or revoked..",
    TooManyRequestsException(),
)

class OuraAccessForbiddenError : OuraErrorBase(
    "Oura subscription has expired or API data not available..",
    TooManyRequestsException(),
)

class OuraValidationError : OuraErrorBase("Invalid Oura data..", TooManyRequestsException())
