package org.radarbase.oura.route

import org.radarbase.oura.user.UserRepository

object OuraRouteFactory {
    
    fun getRoutes(userRepository: UserRepository): List<OuraRoute> {
        return listOf(
            OuraDailyActivityRoute(userRepository),
            OuraDailyReadinessRoute(userRepository),
            OuraDailySleepRoute(userRepository),
            OuraHeartRateRoute(userRepository),
            OuraPersonalInfoRoute(userRepository),
            OuraSessionRoute(userRepository),
            OuraSleepRoute(userRepository),
            OuraTagRoute(userRepository),
            OuraWorkoutRoute(userRepository)
        )
    }
}
