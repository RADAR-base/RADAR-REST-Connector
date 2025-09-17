package org.radarbase.oura.route

data class OuraConfig @JvmOverloads constructor(
    val enabledRoutes: Set<OuraRouteType> = defaultEnabledRoutes(),
) {
    companion object {
        @JvmStatic
        fun defaultEnabledRoutes(): Set<OuraRouteType> = enumValues<OuraRouteType>().toSet()
    }
}
