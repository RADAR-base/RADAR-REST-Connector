package org.radarbase.oura.route

import org.radarbase.oura.user.UserRepository

object OuraRouteFactory {

    fun getRoutes(userRepository: UserRepository): List<OuraRoute> =
        getRoutes(userRepository, OuraRouteFlags())

    fun getRoutes(userRepository: UserRepository, flags: OuraRouteFlags): List<OuraRoute> {
        val routes = mutableListOf<OuraRoute>()
        if (flags.dailyActivityEnabled) routes.add(OuraDailyActivityRoute(userRepository))
        if (flags.dailyReadinessEnabled) routes.add(OuraDailyReadinessRoute(userRepository))
        if (flags.dailySleepEnabled) routes.add(OuraDailySleepRoute(userRepository))
        if (flags.dailyOxygenSaturationEnabled) routes.add(OuraDailyOxygenSaturationRoute(userRepository))
        if (flags.heartRateEnabled) routes.add(OuraHeartRateRoute(userRepository))
        if (flags.personalInfoEnabled) routes.add(OuraPersonalInfoRoute(userRepository))
        if (flags.sessionEnabled) routes.add(OuraSessionRoute(userRepository))
        if (flags.sleepEnabled) routes.add(OuraSleepRoute(userRepository))
        if (flags.tagEnabled) routes.add(OuraTagRoute(userRepository))
        if (flags.workoutEnabled) routes.add(OuraWorkoutRoute(userRepository))
        if (flags.ringConfigurationEnabled) routes.add(OuraRingConfigurationRoute(userRepository))
        if (flags.restModePeriodEnabled) routes.add(OuraRestModePeriodRoute(userRepository))
        if (flags.sleepTimeRecommendationEnabled) routes.add(OuraSleepTimeRecommendationRoute(userRepository))
        return routes
    }
}
