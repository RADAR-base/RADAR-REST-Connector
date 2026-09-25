package org.radarbase.dexcom.request

import okhttp3.Request
import org.radarbase.dexcom.route.DexcomRoute
import org.radarbase.dexcom.user.User
import java.time.Instant

data class RestRequest(
    val request: Request,
    val user: User,
    val route: DexcomRoute,
    val startDate: Instant,
    val endDate: Instant,
)
