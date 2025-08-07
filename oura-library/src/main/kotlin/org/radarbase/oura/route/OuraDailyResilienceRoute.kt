
package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailyResilienceConverter
import org.radarbase.oura.user.UserRepository

class OuraDailyResilienceRoute(
    private val userRepository: UserRepository,
) : OuraRoute(userRepository) {

    override fun subPath(): String = "daily_resilience"

    override fun toString(): String = "oura_daily_resilience"

    override var converters = listOf(OuraDailyResilienceConverter())
}
