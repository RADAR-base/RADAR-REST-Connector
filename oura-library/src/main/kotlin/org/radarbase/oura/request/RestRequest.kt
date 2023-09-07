package org.radarbase.oura.request

import okhttp3.Request
import org.radarbase.oura.route.OuraRoute
import org.radarbase.oura.user.User
import java.time.Instant

data class RestRequest(
    val request: Request,
    val user: User,
    val route: OuraRoute,
    val startDate: Instant,
    val endDate: Instant,
)
