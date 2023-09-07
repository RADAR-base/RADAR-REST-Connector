package org.radarbase.oura.request

import org.radarbase.oura.offset.Offset
import org.radarbase.oura.route.Route
import org.radarbase.oura.user.User
import java.time.Instant

interface OuraOffsetManager {

    fun getOffset(route: Route, user: User): Offset?

    fun updateOffsets(route: Route, user: User, offset: Instant)
}
