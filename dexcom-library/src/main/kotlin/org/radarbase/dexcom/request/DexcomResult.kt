package org.radarbase.dexcom.request

sealed class DexcomResult<out T : Any> {
    data class Success<out T : Any>(val value: T) : DexcomResult<T>()
    data class Error(val error: DexcomError) : DexcomResult<Nothing>()
}

sealed interface DexcomError

sealed class DexcomErrorBase(
    val message: String,
    val cause: Exception? = null,
    val code: String,
) : DexcomError

class DexcomRateLimitError(message: String, cause: Exception? = null, code: String) : DexcomErrorBase(
    message,
    cause,
    code,
)

class DexcomClientException(message: String, cause: Exception? = null, code: String) : DexcomErrorBase(
    message,
    cause,
    code,
)

class DexcomUnauthorizedAccessError(
    message: String,
    cause: Exception? = null,
    code: String,
) : DexcomErrorBase(
    message,
    cause,
    code,
)

class DexcomAccessForbiddenError(
    message: String,
    cause: Exception? = null,
    code: String,
) : DexcomErrorBase(
    message,
    cause,
    code,
)

class DexcomValidationError(message: String, cause: Exception? = null, code: String) : DexcomErrorBase(
    message,
    cause,
    code,
)

class DexcomGenericError(message: String, cause: Exception? = null, code: String) : DexcomErrorBase(
    message,
    cause,
    code,
)

class DexcomNotFoundError(message: String, cause: Exception? = null, code: String) : DexcomErrorBase(
    message,
    cause,
    code,
)
