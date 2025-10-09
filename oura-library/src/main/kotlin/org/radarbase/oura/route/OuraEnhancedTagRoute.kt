package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraEnhancedTagConverter
import org.radarbase.oura.user.UserRepository

class OuraEnhancedTagRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "enhanced_tag"

    override fun toString(): String = "oura_enhanced_tag"

    override var converters = listOf(OuraEnhancedTagConverter())
}
