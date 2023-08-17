package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter

class OuraSessionRoute : OuraRoute() {

    override fun subPath(): String = "session"

    override fun toString(): String = "oura_session"

    override var converter = OuraDailySleepConverter()

}
