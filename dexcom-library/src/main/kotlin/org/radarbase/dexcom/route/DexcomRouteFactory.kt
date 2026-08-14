package org.radarbase.dexcom.route

import org.radarbase.dexcom.user.UserRepository

object DexcomRouteFactory {
    
    fun getRoutes(userRepository: UserRepository): List<DexcomRoute> {
        return listOf(
            DexcomEGVRoute(userRepository),
            DexcomEventsRoute(userRepository),
            DexcomCalibrationsRoute(userRepository),
        )
    }
}