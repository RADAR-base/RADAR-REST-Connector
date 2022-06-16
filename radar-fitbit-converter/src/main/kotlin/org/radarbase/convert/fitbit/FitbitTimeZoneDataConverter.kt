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
package org.radarbase.convert.fitbit

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.fitbit.FitbitTimeZone
import org.slf4j.LoggerFactory
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

class FitbitTimeZoneDataConverter(private val timeZoneTopic: String) : FitbitDataConverter {
    override fun processRecords(
        dateRange: DateRange,
        root: JsonNode,
        timeReceived: Double,
    ): Sequence<Result<TopicData>> {
        val user = root.optObject("user") ?: run {
            return sequenceOf(
                failure(IllegalArgumentException("Failed to get timezone from $root"))
            )
        }
        val offset = user.optInt("offsetFromUTCMillis")?.let { it / 1000 }
        return sequenceOf(
            success(
                TopicData(
                    sourceOffset = dateRange.start.toInstant(),
                    topic = timeZoneTopic,
                    value = FitbitTimeZone(timeReceived, offset),
                )
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FitbitTimeZoneDataConverter::class.java)
    }
}
