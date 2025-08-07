package org.radarbase.oura.route

import org.radarbase.oura.user.UserRepository

object OuraRouteFactory {

    fun getRoutes(userRepository: UserRepository): List<OuraRoute> {
        return listOf(
            OuraDailyActivityRoute(userRepository),
            OuraDailyReadinessRoute(userRepository),
            OuraDailySleepRoute(userRepository),
            OuraDailyOxygenSaturationRoute(userRepository),
            OuraHeartRateRoute(userRepository),
            OuraPersonalInfoRoute(userRepository),
            OuraSessionRoute(userRepository),
            OuraSleepRoute(userRepository),
            OuraTagRoute(userRepository),
            OuraWorkoutRoute(userRepository),
            OuraRingConfigurationRoute(userRepository),
            OuraRestModePeriodRoute(userRepository),
            OuraSleepTimeRecommendationRoute(userRepository),
            OuraDailyStressRoute(userRepository),
            OuraDailyCardiovascularAgeRoute(userRepository),
            OuraDailyResilienceRoute(userRepository),
            OuraEnhancedTagRoute(userRepository),
            OuraVO2MaxRoute(userRepository)
        )
    }
}
