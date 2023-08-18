package org.radarbase.oura.route

import org.radarbase.oura.user.UserRepository

object OuraRouteFactory {
    
    fun getRoute(userRepository: UserRepository): OuraDailySleepRoute {
        return OuraDailySleepRoute(userRepository)
    }
}
