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
import org.radarcns.connector.fitbit.FitbitIntradaySteps
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.time.ZonedDateTime

class FitbitIntradayStepsDataConverter(private val stepTopic: String) : FitbitDataConverter {
    override fun processRecords(
        dateRange: DateRange, root: JsonNode, timeReceived: Double
    ): Sequence<Result<TopicData>> {
        val intraday = root.optObject("activities-steps-intraday")
            ?: return emptySequence()
        val dataset = intraday.optArray("dataset")
            ?: return emptySequence()
        val interval = intraday.getRecordInterval(60)

        // Used as the date to convert the local times in the dataset to absolute times.
        val startDate: ZonedDateTime = dateRange.end
        return dataset.asSequence()
            .mapCatching { activity ->
                val localTime = LocalTime.parse(activity.get("time").asText())
                val time = startDate.with(localTime).toInstant()
                TopicData(
                    sourceOffset = time,
                    topic = stepTopic,
                    value = FitbitIntradaySteps(
                        time.toEpochMilli() / 1000.0,
                        timeReceived,
                        interval,
                        activity.get("value").asInt(),
                    ),
                )
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            FitbitIntradayStepsDataConverter::class.java)
    }
}
