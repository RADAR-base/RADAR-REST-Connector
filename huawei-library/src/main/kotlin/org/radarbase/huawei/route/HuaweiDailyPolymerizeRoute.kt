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

package org.radarbase.huawei.route

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.avro.specific.SpecificRecord
import org.radarbase.huawei.converter.FieldValues
import org.radarbase.huawei.converter.HuaweiDailyPolymerizeConverter
import org.radarbase.huawei.converter.HuaweiDataConverter
import org.radarbase.huawei.request.RestRequest
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Route backed by `POST /healthkit/v2/sampleSet:dailyPolymerize`, used for every Huawei
 * `<type>.statistics` data type.
 *
 * Huawei's `sampleSet:polymerize` endpoint (see [HuaweiSampleSetRoute]) does not accept a
 * `groupByTime`-aggregated query for every data type - some (confirmed live: `resting_calories`)
 * reject it with `"does not support the query mode, please use dailyPolymerize API"`. This route
 * calls that dedicated day-granularity statistics endpoint instead, which takes a day-string range
 * (`startDay`/`endDay`, format `yyyyMMdd`, at most 31 days apart) rather than epoch timestamps.
 *
 * @author yatharthranjan
 */
open class HuaweiDailyPolymerizeRoute(
    userRepository: UserRepository,
    private val dataTypeName: String,
    private val topic: String,
    // dailyPolymerize accepts at most 31 days (inclusive) per request.
    maxIntervalPerRequest: Duration = Duration.ofDays(30L),
    buildRecord: (
        fields: FieldValues,
        startTime: Instant,
        endTime: Instant?,
        timeReceived: Instant,
    ) -> SpecificRecord,
) : HuaweiRoute(userRepository, maxIntervalPerRequest) {

    override val converters: List<HuaweiDataConverter> =
        listOf(HuaweiDailyPolymerizeConverter(topic, buildRecord))

    override fun toString(): String = "huawei_" + topic.removePrefix("connect_huawei_")

    /**
     * Requests whole UTC days only: from the first midnight at or after [start] up to the last
     * midnight at least [COMPLETION_DELAY] ago (capped at [end]), so a day's statistics are only
     * fetched once that day is over and has had time to sync, and consecutive requests never
     * overlap. Each request's `endDay` is inclusive, so it names the day before the exclusive
     * range end.
     */
    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
        max: Int,
    ): Sequence<RestRequest> {
        val firstDay = start.ceilToDay()
        val lastDayEnd = end.coerceAtMost(Instant.now().minus(COMPLETION_DELAY)).floorToDay()
        if (!firstDay.isBefore(lastDayEnd)) return emptySequence()
        return chunkedRanges(firstDay, lastDayEnd, max).map { (rangeStart, rangeEnd) ->
            RestRequest(
                request = createPostRequest(
                    user,
                    "sampleSet:dailyPolymerize",
                    buildRequestBody(rangeStart, rangeEnd.minus(Duration.ofDays(1))),
                    baseUrl = HUAWEI_API_BASE_URL_V2,
                ),
                user = user,
                route = this,
                startDate = rangeStart,
                endDate = rangeEnd,
            )
        }
    }

    private fun buildRequestBody(firstDay: Instant, lastDay: Instant): String {
        val root = MAPPER.createObjectNode()
        root.putArray("dataTypes").add(dataTypeName)
        root.put("startDay", DAY_FORMATTER.format(firstDay))
        root.put("endDay", DAY_FORMATTER.format(lastDay))
        root.put("timeZone", "+0000")
        return MAPPER.writeValueAsString(root)
    }

    companion object {
        /** How long after the end of a UTC day its statistics are first requested. */
        private val COMPLETION_DELAY = Duration.ofHours(12L)
        private val MAPPER = ObjectMapper()

        private fun Instant.floorToDay(): Instant = truncatedTo(ChronoUnit.DAYS)

        private fun Instant.ceilToDay(): Instant = floorToDay().let {
            if (it == this) it else it.plus(Duration.ofDays(1))
        }
        private val DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
    }
}
