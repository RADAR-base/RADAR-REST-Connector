package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter
import org.radarbase.oura.user.UserRepository

class OuraDailySleepRoute(
    private val userRepository: UserRepository?
) : OuraRoute(userRepository) {

    override fun subPath(): String = "daily_sleep"

    override fun toString(): String = "oura_daily_sleep"

    override var converter = OuraDailySleepConverter()

}
