package org.radarbase.dexcom.offset

import org.radarbase.dexcom.route.Route
import org.radarbase.dexcom.user.User
import java.time.Instant

data class Offset(
    val user: User,
    val route: Route,
    val offset: Instant,
)
