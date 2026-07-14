package org.radarbase.huawei.request

sealed class HuaweiResult<out T : Any> {
    data class Success<out T : Any>(val value: T) : HuaweiResult<T>()
    data class Error(val error: HuaweiError) : HuaweiResult<Nothing>()
}

sealed interface HuaweiError

sealed class HuaweiErrorBase(
    val message: String,
    val cause: Exception? = null,
    val code: String,
) : HuaweiError

class HuaweiRateLimitError(
    message: String,
    cause: Exception? = null,
    code: String,
) : HuaweiErrorBase(message, cause, code)

class HuaweiClientException(
    message: String,
    cause: Exception? = null,
    code: String,
) : HuaweiErrorBase(message, cause, code)

class HuaweiUnauthorizedAccessError(
    message: String,
    cause: Exception? = null,
    code: String,
) : HuaweiErrorBase(message, cause, code)

class HuaweiAccessForbiddenError(
    message: String,
    cause: Exception? = null,
    code: String,
) : HuaweiErrorBase(message, cause, code)

class HuaweiValidationError(
    message: String,
    cause: Exception? = null,
    code: String,
) : HuaweiErrorBase(message, cause, code)

class HuaweiGenericError(
    message: String,
    cause: Exception? = null,
    code: String,
) : HuaweiErrorBase(message, cause, code)

class HuaweiNotFoundError(
    message: String,
    cause: Exception? = null,
    code: String,
) : HuaweiErrorBase(message, cause, code)
