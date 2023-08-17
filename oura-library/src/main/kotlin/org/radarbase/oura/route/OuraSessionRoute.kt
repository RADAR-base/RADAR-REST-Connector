package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter
import org.radarbase.oura.user.UserRepository

class OuraSessionRoute(
    private val userRepository: UserRepository?
) : OuraRoute(userRepository) {

    override fun subPath(): String = "session"

    override fun toString(): String = "oura_session"

    override var converter = OuraDailySleepConverter()

}
