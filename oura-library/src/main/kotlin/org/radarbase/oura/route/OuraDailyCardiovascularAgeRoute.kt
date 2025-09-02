package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailyCardiovascularAgeConverter
import org.radarbase.oura.user.UserRepository

class OuraDailyCardiovascularAgeRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "daily_cardiovascular_age"

    override fun toString(): String = "oura_daily_cardiovascular_age"

    override var converters = listOf(OuraDailyCardiovascularAgeConverter())
}
