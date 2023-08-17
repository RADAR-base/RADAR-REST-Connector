package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter
import org.radarbase.oura.user.UserRepository

class OuraDailyActivityRoute(
    private val userRepository: UserRepository?
) : OuraRoute(userRepository) {

    override fun subPath(): String = "daily_activity"

    override fun toString(): String = "oura_daily_activity"

    override var converter = OuraDailySleepConverter()

}
