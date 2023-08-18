package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter
import org.radarbase.oura.user.UserRepository

class OuraWorkoutRoute(
    private val userRepository: UserRepository?
) : OuraRoute(userRepository) {

    override fun subPath(): String = "workout"

    override fun toString(): String = "oura_workout"

    override var converter = OuraDailySleepConverter()

}
