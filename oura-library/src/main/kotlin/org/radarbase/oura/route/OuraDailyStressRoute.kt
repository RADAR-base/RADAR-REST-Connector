package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailyStressConverter
import org.radarbase.oura.user.UserRepository

class OuraDailyStressRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "daily_stress"

    override fun toString(): String = "oura_daily_stress"

    override var converters = listOf(OuraDailyStressConverter())
}
