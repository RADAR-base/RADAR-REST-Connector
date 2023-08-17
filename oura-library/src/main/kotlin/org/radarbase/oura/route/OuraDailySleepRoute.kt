package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter

class OuraDailySleepRoute : OuraRoute() {

    override fun subPath(): String = "daily_sleep"

    override fun toString(): String = "oura_daily_sleep"

    override var converter = OuraDailySleepConverter()

}
