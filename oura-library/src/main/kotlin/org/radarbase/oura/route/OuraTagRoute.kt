package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraTagConverter
import org.radarbase.oura.user.UserRepository

class OuraTagRoute(
    private val userRepository: UserRepository
) : OuraRoute(userRepository) {

    override fun subPath(): String = "tag"

    override fun toString(): String = "oura_tag"

    override var converters = listOf(OuraTagConverter())

}
