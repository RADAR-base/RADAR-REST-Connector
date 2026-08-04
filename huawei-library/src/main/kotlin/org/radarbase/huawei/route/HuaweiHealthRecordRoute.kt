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

import org.apache.avro.specific.SpecificRecord
import org.radarbase.huawei.converter.FieldValues
import org.radarbase.huawei.converter.HuaweiDataConverter
import org.radarbase.huawei.converter.HuaweiHealthRecordConverter
import org.radarbase.huawei.request.RestRequest
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserRepository
import java.time.Duration
import java.time.Instant

/**
 * Route backed by `GET /healthkit/v2/healthRecords`, used for the `health.record.*` data types
 * (ambulatory blood pressure sessions, heart rate alerts, hyperthermia, low SpO2 alerts,
 * menstrual cycle phases, and comprehensive sleep records).
 *
 * Per the official Health Kit REST API reference, this endpoint is on API version `v2` (unlike
 * `sampleSet:polymerize`/`activityRecords`, which are on `v1`), takes the data type under the
 * `dataType` query parameter (not `dataTypeName`), and its `startTime`/`endTime` parameters (and
 * the `startTime`/`endTime` fields of each returned record) are in **nanoseconds** since the
 * epoch, not milliseconds.
 *
 * @author yatharthranjan
 */
open class HuaweiHealthRecordRoute(
    userRepository: UserRepository,
    private val dataTypeName: String,
    private val topic: String,
    maxIntervalPerRequest: Duration = Duration.ofDays(30L),
    buildRecord: (
        fields: FieldValues,
        startTime: Instant,
        endTime: Instant?,
        timeReceived: Instant,
    ) -> SpecificRecord,
) : HuaweiRoute(userRepository, maxIntervalPerRequest) {

    override val converters: List<HuaweiDataConverter> =
        listOf(HuaweiHealthRecordConverter(topic, buildRecord))

    override fun toString(): String = "huawei_" + topic.removePrefix("connect_huawei_")

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
        max: Int,
    ): Sequence<RestRequest> = chunkedRanges(start, end, max).map { (rangeStart, rangeEnd) ->
        RestRequest(
            request = createGetRequest(
                user,
                "healthRecords",
                mapOf(
                    "dataType" to dataTypeName,
                    "startTime" to rangeStart.toEpochNanos().toString(),
                    "endTime" to rangeEnd.toEpochNanos().toString(),
                ),
                baseUrl = HUAWEI_API_BASE_URL_V2,
            ),
            user = user,
            route = this,
            startDate = rangeStart,
            endDate = rangeEnd,
        )
    }

    private fun Instant.toEpochNanos(): Long = epochSecond * 1_000_000_000L + nano.toLong()
}
