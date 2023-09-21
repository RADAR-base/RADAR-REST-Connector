package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraRestModePeriodConverter
import org.radarbase.oura.converter.OuraRestModeTagConverter
import org.radarbase.oura.user.UserRepository

class OuraRestModePeriodRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "rest_mode_period"

    override fun toString(): String = "oura_rest_mode_period"

    override var converters = listOf(
        OuraRestModePeriodConverter(),
        OuraRestModeTagConverter()
        )
}
