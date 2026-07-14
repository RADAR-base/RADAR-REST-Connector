package org.radarbase.huawei.request

import okhttp3.Request
import org.radarbase.huawei.route.HuaweiRoute
import org.radarbase.huawei.user.User
import java.time.Instant

data class RestRequest(
    val request: Request,
    val user: User,
    val route: HuaweiRoute,
    val startDate: Instant,
    val endDate: Instant,
)
