package org.radarbase.oura.request

import okhttp3.Response
import org.radarbase.oura.route.Route
import org.radarbase.oura.user.User
import org.radarbase.oura.offset.Offsets
import org.radarbase.oura.offset.Offset
import java.time.Instant

interface OuraOffsetManager {

    fun getOffset(route: Route, user: User): Offset

    fun updateOffsets(route: Route, user: User, offset: Instant)
}
