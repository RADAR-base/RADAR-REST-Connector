package org.radarbase.oura.offset

import org.radarbase.oura.route.Route
import org.radarbase.oura.user.User
import java.time.Instant

data class Offset(
    val user: User,
    val route: Route,
    val offset: Instant,
)
