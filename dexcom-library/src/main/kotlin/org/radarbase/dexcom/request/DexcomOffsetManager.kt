package org.radarbase.dexcom.request

import org.radarbase.dexcom.route.Route
import org.radarbase.dexcom.user.User
import java.time.Instant

data class Offset(
    val user: User,
    val route: Route,
    val offset: Instant,
)

interface DexcomOffsetManager {

    fun getOffset(route: Route, user: User): Offset?

    fun updateOffsets(route: Route, user: User, offset: Instant)
}
