package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter

class OuraDailyReadinessRoute : OuraRoute() {

    override fun subPath(): String = "daily_readiness"

    override fun toString(): String = "oura_daily_readiness"

    override var converter = OuraDailySleepConverter()

}
