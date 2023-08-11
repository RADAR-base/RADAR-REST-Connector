package org.radarbase.oura.offset

import java.time.Instant

data class Offset(
            val userId: String,
            val route: String,
            val offset: Instant
)