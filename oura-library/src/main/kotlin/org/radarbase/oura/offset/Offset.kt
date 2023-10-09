package org.radarbase.oura.offset

import java.time.Instant
import org.radarbase.oura.user.User
import org.radarbase.oura.route.Route

data class Offset(
    val user: User,
    val route: Route,
    val offset: Instant,
)
