package org.radarbase.huawei.route

import org.radarbase.huawei.converter.HuaweiActivityRecordConverter
import org.radarbase.huawei.converter.HuaweiDataConverter
import org.radarbase.huawei.request.RestRequest
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserRepository
import java.time.Duration
import java.time.Instant

/**
 * Route backed by `GET /healthkit/v1/activityRecords`, covering the Huawei Health Kit Activity
 * Records API (workout / physical-activity sessions).
 */
class HuaweiActivityRecordRoute(
    userRepository: UserRepository,
    private val topic: String = "connect_huawei_activity_record",
    maxIntervalPerRequest: Duration = Duration.ofDays(30L),
) : HuaweiRoute(userRepository, maxIntervalPerRequest) {

    override val converters: List<HuaweiDataConverter> = listOf(HuaweiActivityRecordConverter(topic))

    override fun toString(): String = "huawei_activity_record"

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
        max: Int,
    ): Sequence<RestRequest> = chunkedRanges(start, end, max).map { (rangeStart, rangeEnd) ->
        RestRequest(
            request = createGetRequest(
                user,
                "activityRecords",
                mapOf(
                    "startTime" to rangeStart.toEpochMilli().toString(),
                    "endTime" to rangeEnd.toEpochMilli().toString(),
                ),
            ),
            user = user,
            route = this,
            startDate = rangeStart,
            endDate = rangeEnd,
        )
    }
}
