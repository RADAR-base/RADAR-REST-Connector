package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter
import org.radarbase.oura.user.UserRepository

class OuraSleepRoute(
    private val userRepository: UserRepository?
) : OuraRoute(userRepository) {

    override fun subPath(): String = "sleep"

    override fun toString(): String = "oura_sleep"

    override var converter = OuraDailySleepConverter()

}
