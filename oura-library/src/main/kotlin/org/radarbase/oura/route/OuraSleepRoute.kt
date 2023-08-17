package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter

class OuraSleepRoute : OuraRoute() {

    override fun subPath(): String = "sleep"

    override fun toString(): String = "oura_sleep"

    override var converter = OuraDailySleepConverter()

}
