package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter

class OuraPersonalInfoRoute : OuraRoute() {

    override fun subPath(): String = "personal_info"

    override fun toString(): String = "oura_personal_info"

    override var converter = OuraDailySleepConverter()

}
