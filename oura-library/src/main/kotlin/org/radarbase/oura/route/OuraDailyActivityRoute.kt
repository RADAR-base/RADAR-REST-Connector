package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailyActivityConverter
import org.radarbase.oura.converter.OuraDailyActivityMetConverter
import org.radarbase.oura.converter.OuraDailyActivityClassConverter
import org.radarbase.oura.user.UserRepository

class OuraDailyActivityRoute(
    private val userRepository: UserRepository
) : OuraRoute(userRepository) {

    override fun subPath(): String = "daily_activity"

    override fun toString(): String = "oura_daily_activity"

    override var converters = listOf(
        OuraDailyActivityConverter(),
        OuraDailyActivityMetConverter(),
        OuraDailyActivityClassConverter()
        )

}
