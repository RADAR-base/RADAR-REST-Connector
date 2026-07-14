package org.radarbase.huawei.request

import org.radarbase.huawei.offset.Offset
import org.radarbase.huawei.route.Route
import org.radarbase.huawei.user.User
import java.time.Instant

interface HuaweiOffsetManager {

    fun getOffset(route: Route, user: User): Offset?

    fun updateOffsets(route: Route, user: User, offset: Instant)
}
