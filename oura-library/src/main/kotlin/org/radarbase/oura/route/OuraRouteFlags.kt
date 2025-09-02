package org.radarbase.oura.route

data class OuraRouteFlags(
    val dailyActivityEnabled: Boolean = true,
    val dailyReadinessEnabled: Boolean = true,
    val dailySleepEnabled: Boolean = true,
    val dailyOxygenSaturationEnabled: Boolean = true,
    val heartRateEnabled: Boolean = true,
    val personalInfoEnabled: Boolean = true,
    val sessionEnabled: Boolean = true,
    val sleepEnabled: Boolean = true,
    val tagEnabled: Boolean = true,
    val workoutEnabled: Boolean = true,
    val ringConfigurationEnabled: Boolean = true,
    val restModePeriodEnabled: Boolean = true,
    val sleepTimeRecommendationEnabled: Boolean = true,
)
