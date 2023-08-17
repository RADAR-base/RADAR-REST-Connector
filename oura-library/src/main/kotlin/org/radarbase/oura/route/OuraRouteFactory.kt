package org.radarbase.oura.route

import org.radarbase.oura.user.UserRepository

object OuraRouteFactory {

    @Volatile
    private var userRepository: UserRepository? = null
    
    fun getRoute(): OuraDailySleepRoute {
        return OuraDailySleepRoute(userRepository)
    }
}
