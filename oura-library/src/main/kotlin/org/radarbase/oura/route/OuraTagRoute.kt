package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter

class OuraTagRoute : OuraRoute() {

    override fun subPath(): String = "tag"

    override fun toString(): String = "oura_tag"

    override var converter = OuraDailySleepConverter()

}
