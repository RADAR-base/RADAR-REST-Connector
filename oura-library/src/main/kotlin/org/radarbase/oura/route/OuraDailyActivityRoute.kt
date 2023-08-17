package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDailySleepConverter

class OuraDailyActivityRoute : OuraRoute() {

    override fun subPath(): String = "daily_activity"

    override fun toString(): String = "oura_daily_activity"

    override var converter = OuraDailySleepConverter()

}
