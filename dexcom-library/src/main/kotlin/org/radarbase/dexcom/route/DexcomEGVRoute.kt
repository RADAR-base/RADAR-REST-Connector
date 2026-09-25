package org.radarbase.dexcom.route

import org.radarbase.dexcom.converter.DexcomDataConverter
import org.radarbase.dexcom.converter.DexcomEGVConverter
import org.radarbase.dexcom.user.UserRepository

class DexcomEGVRoute(
    userRepository: UserRepository,
    apiBaseUrl: String = DEFAULT_API_BASE_URL,
) : DexcomRoute(userRepository, apiBaseUrl) {
    override fun subPath(): String = "egvs"

    override fun toString(): String = "dexcom_egv"

    override val converters: List<DexcomDataConverter> = listOf(DexcomEGVConverter())
}