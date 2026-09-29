package org.radarbase.dexcom.route

import org.radarbase.dexcom.converter.DexcomCalibrationsConverter
import org.radarbase.dexcom.converter.DexcomDataConverter
import org.radarbase.dexcom.user.UserRepository

class DexcomCalibrationsRoute(
    userRepository: UserRepository,
    apiBaseUrl: String = DEFAULT_API_BASE_URL,
) : DexcomRoute(userRepository, apiBaseUrl) {
    override fun subPath(): String = "calibrations"

    override fun toString(): String = "dexcom_calibration"

    override val converters: List<DexcomDataConverter> = listOf(DexcomCalibrationsConverter())
}
