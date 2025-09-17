package org.radarbase.oura.route

import org.radarbase.oura.user.UserRepository

object OuraRouteFactory {

    @JvmStatic
    fun getRoutes(userRepository: UserRepository): List<Route> =
        getRoutes(userRepository, OuraConfig())

    @JvmStatic
    fun getRoutes(userRepository: UserRepository, config: OuraConfig): List<Route> {
        val routes = mutableListOf<Route>()

        // Helper to add based on enum presence
        fun isEnabled(type: OuraRouteType): Boolean =
            type in config.enabledRoutes

        if (isEnabled(OuraRouteType.DAILY_ACTIVITY)) {
            routes.add(OuraDailyActivityRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.DAILY_READINESS)) {
            routes.add(OuraDailyReadinessRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.DAILY_SLEEP)) {
            routes.add(OuraDailySleepRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.DAILY_OXYGEN_SATURATION)) {
            routes.add(OuraDailyOxygenSaturationRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.HEART_RATE)) {
            routes.add(OuraHeartRateRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.PERSONAL_INFO)) {
            routes.add(OuraPersonalInfoRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.SESSION)) {
            routes.add(OuraSessionRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.SLEEP)) {
            routes.add(OuraSleepRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.TAG)) {
            routes.add(OuraTagRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.WORKOUT)) {
            routes.add(OuraWorkoutRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.RING_CONFIGURATION)) {
            routes.add(OuraRingConfigurationRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.REST_MODE_PERIOD)) {
            routes.add(OuraRestModePeriodRoute(userRepository))
        }
        if (isEnabled(OuraRouteType.SLEEP_TIME_RECOMMENDATION)) {
            routes.add(OuraSleepTimeRecommendationRoute(userRepository))
        }
        return routes
    }
}
