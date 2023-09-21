package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraRingConfigurationConverter
import org.radarbase.oura.user.UserRepository

class OuraRingConfigurationRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "ring_configuration"

    override fun toString(): String = "oura_ring_configuration"

    override var converters = listOf(OuraRingConfigurationConverter())
}
