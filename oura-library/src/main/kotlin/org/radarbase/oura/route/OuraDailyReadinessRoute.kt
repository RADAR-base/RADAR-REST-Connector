package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter
import org.radarbase.oura.user.UserRepository

class OuraDailyReadinessRoute(
    private val userRepository: UserRepository?
) : OuraRoute(userRepository) {

    override fun subPath(): String = "daily_readiness"

    override fun toString(): String = "oura_daily_readiness"

    override var converter = OuraDailySleepConverter()

}
