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
import org.radarcns.connector.fitbit.FitbitSleepClassic
import org.radarcns.connector.fitbit.FitbitSleepClassicLevel
import org.radarcns.connector.fitbit.FitbitSleepStage
import org.radarcns.connector.fitbit.FitbitSleepStageLevel
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

class FitbitSleepDataConverter(
    private val sleepStagesTopic: String,
    private val sleepClassicTopic: String,
) : FitbitDataConverter {
    override fun processRecords(
        context: ConverterContext, root: JsonNode, timeReceived: Double,
    ): Sequence<Result<TopicData>> {
        val meta = root.optObject("meta")
        if (meta != null && meta["state"]?.asText() == "pending") {
            return emptySequence()
        }
        val sleepArray = root.optArray("sleep")
            ?: return emptySequence()
        return sleepArray.asSequence()
            .sortedBy { s -> s.get("startTime").asText() }
            .mapCatching { s ->
                val startTime = Instant.from(DATE_TIME_FORMAT.parse(s.get("startTime").asText()))
                val type = s.optString("type")
                val isStages = type == null || type == "stages"

                // use an intermediate offset for all records but the last. Since the query time
                // depends only on the start time of a sleep stages group, this will reprocess the entire
                // sleep stages group if something goes wrong while processing.
                val intermediateOffset = startTime.minus(Duration.ofSeconds(1))
                val allRecords: List<TopicData> = s.optObject("levels")
                    ?.optArray("data")
                    ?.map { d ->
                        val dateTime: String = d.get("dateTime").asText()
                        val duration: Int = d.get("seconds").asInt()
                        val level: String = d.get("level").asText()
                        if (isStages) {
                            TopicData(
                                sourceOffset = intermediateOffset,
                                topic = sleepStagesTopic,
                                value = FitbitSleepStage(
                                    dateTime,
                                    timeReceived,
                                    duration,
                                    level.toStagesLevel()
                                ),
                            )
                        } else {
                            TopicData(
                                sourceOffset = intermediateOffset,
                                topic = sleepClassicTopic,
                                value = FitbitSleepClassic(
                                    dateTime,
                                    timeReceived,
                                    duration,
                                    level.toClassicLevel(),
                                )
                            )
                        }
                    }
                    ?: emptyList()

                // The final group gets the actual offset, to ensure that the group does not get queried
                // again.
                allRecords.lastOrNull()?.sourceOffset = startTime
                allRecords
            }
            .flatMap { res ->
                res.fold(
                    onSuccess = { data -> data.asSequence().map { success(it) } },
                    onFailure = { sequenceOf(failure(it)) },
                )
            }
    }

    companion object {
        private val DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(ZoneOffset.UTC)


        private fun String.toClassicLevel() = when (this) {
            "awake" -> FitbitSleepClassicLevel.AWAKE
            "asleep" -> FitbitSleepClassicLevel.ASLEEP
            "restless" -> FitbitSleepClassicLevel.RESTLESS
            else -> FitbitSleepClassicLevel.UNKNOWN
        }

        private fun String.toStagesLevel() = when (this) {
            "wake" -> FitbitSleepStageLevel.AWAKE
            "rem" -> FitbitSleepStageLevel.REM
            "deep" -> FitbitSleepStageLevel.DEEP
            "light" -> FitbitSleepStageLevel.LIGHT
            else -> FitbitSleepStageLevel.UNKNOWN
        }
    }
}
