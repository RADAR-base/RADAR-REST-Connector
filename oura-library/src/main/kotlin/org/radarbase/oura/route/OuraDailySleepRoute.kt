package org.radarbase.oura.route

class OuraDailySleepRoute: OuraRoute() {

    override fun subPath(): String = "daily_sleep"

    override fun toString(): String = "oura_daily_sleep"
}