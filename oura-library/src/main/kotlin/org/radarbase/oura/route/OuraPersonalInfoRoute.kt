package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraPersonalInfoConverter
import org.radarbase.oura.user.UserRepository

class OuraPersonalInfoRoute(
    private val userRepository: UserRepository?
) : OuraRoute(userRepository) {

    override fun subPath(): String = "personal_info"

    override fun toString(): String = "oura_personal_info"

    override var converter = OuraPersonalInfoConverter()

}
