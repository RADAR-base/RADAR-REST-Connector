package org.radarbase.dexcom.route

import org.radarbase.dexcom.converter.DexcomAlertsConverter
import org.radarbase.dexcom.converter.DexcomDataConverter
import org.radarbase.dexcom.user.UserRepository

class DexcomAlertsRoute(
    userRepository: UserRepository,
    apiBaseUrl: String = DEFAULT_API_BASE_URL,
) : DexcomRoute(userRepository, apiBaseUrl) {
    override fun subPath(): String = "alerts"

    override fun toString(): String = "dexcom_alert"

    override val converters: List<DexcomDataConverter> = listOf(DexcomAlertsConverter())
}
