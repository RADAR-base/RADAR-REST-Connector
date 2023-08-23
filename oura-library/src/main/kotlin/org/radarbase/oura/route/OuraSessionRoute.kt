package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraSessionConverter
import org.radarbase.oura.converter.OuraMotionCountConverter
import org.radarbase.oura.converter.OuraSessionHrvConverter
import org.radarbase.oura.converter.OuraSessionHeartRateConverter
import org.radarbase.oura.user.UserRepository

class OuraSessionRoute(
    private val userRepository: UserRepository
) : OuraRoute(userRepository) {

    override fun subPath(): String = "session"

    override fun toString(): String = "oura_session"

    override var converters = listOf(
        OuraSessionConverter(), 
        OuraMotionCountConverter(),
        OuraSessionHrvConverter(),
        OuraSessionHeartRateConverter(),
        )

}
