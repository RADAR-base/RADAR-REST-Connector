package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraSleepTimeRecommendationConverter
import org.radarbase.oura.user.UserRepository

class OuraSleepTimeRecommendationRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "sleep_time"

    override fun toString(): String = "oura_sleep_time_recommendation"

    override var converters = listOf(OuraSleepTimeRecommendationConverter())
}
