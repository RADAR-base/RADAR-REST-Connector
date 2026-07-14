package org.radarbase.huawei.offset

import org.radarbase.huawei.route.Route
import org.radarbase.huawei.user.User
import java.time.Instant

data class Offset(
    val user: User,
    val route: Route,
    val offset: Instant,
)
