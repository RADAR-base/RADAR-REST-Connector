package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraHeartRateConverter
import org.radarbase.oura.user.UserRepository

class OuraHeartRateRoute(
    private val userRepository: UserRepository?
) : OuraRoute(userRepository) {

    override fun subPath(): String = "heartrate"

    override fun toString(): String = "oura_heart_rate"

    override var converter = OuraHeartRateConverter()

}
