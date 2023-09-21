package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailyOxygenSaturationConverter
import org.radarbase.oura.user.UserRepository

class OuraDailyOxygenSaturationRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "daily_spo2"

    override fun toString(): String = "oura_daily_oxygen_saturation"

    override var converters = listOf(OuraDailyOxygenSaturationConverter())
}
