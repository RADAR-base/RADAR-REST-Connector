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
import org.radarbase.huawei.converter.HuaweiDataConverter
import org.radarbase.huawei.converter.HuaweiSampleSetConverter
import org.radarbase.huawei.request.RestRequest
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserRepository
import java.time.Duration
import java.time.Instant

/**
 * Route backed by `POST /healthkit/v1/sampleSet:polymerize`, which covers the large majority of
 * Huawei Health Kit data types (all `continuous.*`, `instantaneous.*`, `cgm_blood_glucose`,
 * `active_hours`, `daily_activity_summary`, `emotion`, `heart_rate_variability`, `vo2max`,
 * `resting_calories.statistics`, `sleep.on_off_bed_record`, and `sleep_respiratory_*` types).
 *
 * When [groupByTimeUnit] is set, the request aggregates sample points into buckets of that size —
 * this is how Huawei's `<type>.statistics` data types are queried. When it is `null`, the endpoint
 * returns raw, un-aggregated sample points for [dataTypeName] over the requested time range.
 */
open class HuaweiSampleSetRoute(
    userRepository: UserRepository,
    private val dataTypeName: String,
    private val topic: String,
    private val groupByTimeUnit: String? = null,
    maxIntervalPerRequest: Duration = Duration.ofDays(30L),
    buildRecord: (
        fields: FieldValues,
        startTime: Instant,
        endTime: Instant?,
        timeReceived: Instant,
    ) -> SpecificRecord,
) : HuaweiRoute(userRepository, maxIntervalPerRequest) {

    override val converters: List<HuaweiDataConverter> =
        listOf(HuaweiSampleSetConverter(topic, buildRecord))

    override fun toString(): String = "huawei_" + topic.removePrefix("connect_huawei_")

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
        max: Int,
    ): Sequence<RestRequest> = chunkedRanges(start, end, max).map { (rangeStart, rangeEnd) ->
        RestRequest(
            request = createPostRequest(
                user,
                "sampleSet:polymerize",
                buildRequestBody(rangeStart, rangeEnd),
            ),
            user = user,
            route = this,
            startDate = rangeStart,
            endDate = rangeEnd,
        )
    }

    private fun buildRequestBody(start: Instant, end: Instant): String {
        val root = MAPPER.createObjectNode()
        root.putArray("polymerizeWith").addObject().put("dataTypeName", dataTypeName)
        root.put("startTime", start.toEpochMilli())
        root.put("endTime", end.toEpochMilli())
        if (groupByTimeUnit != null) {
            val groupPeriod = root.putObject("groupByTime").putObject("groupPeriod")
            groupPeriod.put("unit", groupByTimeUnit)
            groupPeriod.put("value", 1)
            groupPeriod.put("timeZone", "+0000")
        }
        return MAPPER.writeValueAsString(root)
    }

    companion object {
        private val MAPPER = ObjectMapper()
    }
}
