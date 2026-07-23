/*
 * Copyright 2026 Onsentia
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
