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
package org.radarbase.huawei.converter

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant

/**
 * Reads an epoch timestamp field from a Huawei response node, whatever its unit.
 *
 * Huawei's endpoints are inconsistent about timestamp units - `activityRecords` uses milliseconds,
 * `healthRecords` and sample points use nanoseconds, and `dailyPolymerize` groups use milliseconds
 * around nanosecond sample points - and the docs don't always say which. Any plausible timestamp
 * (after 2001) differs by at least three orders of magnitude between units, so the unit is
 * inferred from the value's magnitude instead of hard-coded per endpoint. Both numeric and
 * stringified numbers are accepted.
 *
 * @author yatharthranjan
 */
internal fun JsonNode.epochInstant(field: String): Instant? {
    val node = this.get(field) ?: return null
    if (node.isNull) return null
    val value = if (node.isTextual) node.asText().trim().toLongOrNull() else node.asLong()
    return value?.let { epochInstantOf(it) }
}

internal fun epochInstantOf(value: Long): Instant = when {
    value >= NANOS_THRESHOLD -> Instant.ofEpochSecond(
        Math.floorDiv(value, 1_000_000_000L),
        Math.floorMod(value, 1_000_000_000L),
    )
    value >= MICROS_THRESHOLD -> Instant.ofEpochSecond(
        Math.floorDiv(value, 1_000_000L),
        Math.floorMod(value, 1_000_000L) * 1_000L,
    )
    value >= MILLIS_THRESHOLD -> Instant.ofEpochMilli(value)
    else -> Instant.ofEpochSecond(value)
}

// 1e9 s is 2001-09-09, so any post-2001 time is >= 1e12 in ms, >= 1e15 in us and >= 1e18 in ns.
// Lower thresholds by 10x to keep boundaries far from any realistic value in the adjacent unit.
private const val MILLIS_THRESHOLD = 100_000_000_000L
private const val MICROS_THRESHOLD = 100_000_000_000_000L
private const val NANOS_THRESHOLD = 100_000_000_000_000_000L
