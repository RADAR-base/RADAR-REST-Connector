package org.radarbase.dexcom.route

import org.radarbase.dexcom.converter.DexcomDataConverter
import org.radarbase.dexcom.converter.DexcomDevicesConverter
import org.radarbase.dexcom.request.RestRequest
import org.radarbase.dexcom.user.User
import org.radarbase.dexcom.user.UserRepository
import java.time.Instant

/**
 * Devices endpoint has no startDate/endDate query params.
 */
class DexcomDevicesRoute(
    userRepository: UserRepository,
    private val devicesApiBaseUrl: String = DEFAULT_API_BASE_URL,
) : DexcomRoute(userRepository, devicesApiBaseUrl) {
    override fun subPath(): String = "devices"

    override fun toString(): String = "dexcom_device"

    override val converters: List<DexcomDataConverter> = listOf(DexcomDevicesConverter())

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
    ): Sequence<RestRequest> {
        val request = createRequest(user, "$devicesApiBaseUrl/${subPath()}", "")
        return sequenceOf(RestRequest(request, user, this, start, end))
    }

    override fun generateRequests(
        user: User,
        start: Instant,
        end: Instant,
        max: Int,
    ): Sequence<RestRequest> = generateRequests(user, start, end).take(max)
}
