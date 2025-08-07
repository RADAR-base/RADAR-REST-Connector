package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraVO2MaxConverter
import org.radarbase.oura.user.UserRepository

class OuraVO2MaxRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "vo2_max"

    override fun toString(): String = "oura_vo2_max"

    override var converters = listOf(OuraVO2MaxConverter())
}