package org.radarbase.oura.request

sealed class OuraResult<out T : Any> {
    data class Success<out T : Any>(val value: T) : OuraResult<T>()
    data class Error(val error: OuraError) : OuraResult<Nothing>()
}

sealed interface OuraError

sealed class OuraErrorBase(
    val message: String,
    val cause: Exception? = null,
    val code: String,
) : OuraError

class OuraRateLimitError(message: String, cause: Exception? = null, code: String) : OuraErrorBase(
    message,
    cause,
    code,
)

class OuraClientException(message: String, cause: Exception? = null, code: String) : OuraErrorBase(
    message,
    cause,
    code,
)

class OuraUnauthorizedAccessError(
    message: String,
    cause: Exception? = null,
    code: String,
) : OuraErrorBase(
    message,
    cause,
    code,
)

class OuraAccessForbiddenError(
    message: String,
    cause: Exception? = null,
    code: String,
) : OuraErrorBase(
    message,
    cause,
    code,
)

class OuraValidationError(message: String, cause: Exception? = null, code: String) : OuraErrorBase(
    message,
    cause,
    code,
)

class OuraGenericError(message: String, cause: Exception? = null, code: String) : OuraErrorBase(
    message,
    cause,
    code,
)

class OuraNotFoundError(message: String, cause: Exception? = null, code: String) : OuraErrorBase(
    message,
    cause,
    code,
)
