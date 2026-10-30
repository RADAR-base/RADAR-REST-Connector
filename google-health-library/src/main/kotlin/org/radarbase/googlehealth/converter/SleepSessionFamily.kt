/*
 * Copyright 2026 King's College London
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.googlehealth.converter

import com.fasterxml.jackson.databind.JsonNode

internal enum class SleepSessionFamily { STAGES, CLASSIC, UNKNOWN }

/**
 * Resolves which sleep family a session belongs to so STAGES and CLASSIC records route to
 * separate topics.
 *
 * This prefer the session-level `Sleep.type` (CLASSIC | STAGES) when present, and fall
 * back to inferring from the per-stage `type` values when it is absent.
 *
 * AWAKE is deliberately excluded from the fallback signal: it appears in both families, so it
 * cannot discriminate. Once the family is known, AWAKE routes with the rest of its session.
 */
internal object SleepSession {
    private val STAGES_ONLY = setOf("DEEP", "LIGHT", "REM")
    private val CLASSIC_ONLY = setOf("ASLEEP", "RESTLESS")

    fun familyOf(sleep: JsonNode): SleepSessionFamily {
        when (sleep["type"]?.asText()) {
            "STAGES" -> return SleepSessionFamily.STAGES
            "CLASSIC" -> return SleepSessionFamily.CLASSIC
        }
        val stages = sleep["stages"]?.takeIf { it.isArray } ?: return SleepSessionFamily.UNKNOWN
        val hasStages = stages.any { it["type"]?.asText() in STAGES_ONLY }
        val hasClassic = stages.any { it["type"]?.asText() in CLASSIC_ONLY }
        return when {
            hasStages -> SleepSessionFamily.STAGES
            hasClassic -> SleepSessionFamily.CLASSIC
            else -> SleepSessionFamily.UNKNOWN
        }
    }
}
