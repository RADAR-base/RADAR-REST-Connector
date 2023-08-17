package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter

class OuraHeartRateRoute : OuraRoute() {

    override fun subPath(): String = "heartrate"

    override fun toString(): String = "oura_heart_rate"

    override var converter = OuraDailySleepConverter()

}
