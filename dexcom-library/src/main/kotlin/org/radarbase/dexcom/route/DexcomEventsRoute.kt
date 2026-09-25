package org.radarbase.dexcom.route

import org.radarbase.dexcom.converter.DexcomDataConverter
import org.radarbase.dexcom.converter.DexcomEventsConverter
import org.radarbase.dexcom.user.UserRepository

class DexcomEventsRoute(
    userRepository: UserRepository,
    apiBaseUrl: String = DEFAULT_API_BASE_URL,
) : DexcomRoute(userRepository, apiBaseUrl) {
    override fun subPath(): String = "events"

    override fun toString(): String = "dexcom_event"

    override val converters: List<DexcomDataConverter> = listOf(DexcomEventsConverter())
}
