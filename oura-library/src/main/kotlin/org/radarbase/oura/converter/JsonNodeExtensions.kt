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

package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("org.radarbase.oura.converter.JsonNodeExtensions")

internal fun JsonNode.getRecordInterval(defaultValue: Int): Int {
    val type = this["datasetType"]
    val interval = this["datasetInterval"]
    if (type == null || interval == null) {
        logger.warn("Failed to get data interval; using {} instead", defaultValue)
        return defaultValue
    }
    return when (type.asText()) {
        "minute" -> TimeUnit.MINUTES
        "second" -> TimeUnit.SECONDS
        "hour" -> TimeUnit.HOURS
        "day" -> TimeUnit.DAYS
        "millisecond" -> TimeUnit.MILLISECONDS
        "nanosecond" -> TimeUnit.NANOSECONDS
        "microsecond" -> TimeUnit.MICROSECONDS
        else -> {
            logger.warn(
                    "Failed to parse dataset interval type {} for {}; using {} seconds instead",
                    type.asText(),
                    interval.asLong(),
                    defaultValue
            )
            return defaultValue
        }
    }.toSeconds(interval.asLong()).toInt()
}

internal fun JsonNode.optLong(fieldName: String): Long? = this[fieldName]
        ?.takeIf { it.canConvertToLong() }
        ?.longValue()

internal fun JsonNode.optDouble(fieldName: String): Double? = this[fieldName]
        ?.takeIf { it.isNumber }
        ?.doubleValue()

internal fun JsonNode.optFloat(fieldName: String): Float? = this[fieldName]
        ?.takeIf { it.isNumber }
        ?.floatValue()

internal fun JsonNode.optInt(fieldName: String): Int? = this[fieldName]
        ?.takeIf { it.canConvertToInt() }
        ?.intValue()

internal fun JsonNode.optString(fieldName: String?): String? = this[fieldName]
        ?.takeIf { it.isTextual }
        ?.textValue()

internal fun JsonNode.optBoolean(fieldName: String?): Boolean? = this[fieldName]
        ?.takeIf { it.isBoolean }
        ?.booleanValue()

internal fun JsonNode.optObject(fieldName: String?): ObjectNode? = this[fieldName]
        ?.takeIf { it.isObject } as ObjectNode?

internal fun JsonNode.optArray(fieldName: String?): Iterable<JsonNode>? = this[fieldName]
        ?.takeIf { it.isArray && it.size() > 0 }


internal fun <S, T> Sequence<T>.mapCatching(fn: (T) -> S): Sequence<Result<S>> = map { t ->
    runCatching {
        fn(t)
    }
}