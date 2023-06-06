package org.radarbase.oura.route

import org.radarbase.oura.converter.OuraDataConverter
import org.radarbase.oura.request.OuraRequest

open class OuraDailySleepRoute: OuraRoute {
    val name = 'oura_daily_sleep'

    override fun converter(): OuraDataConverter
    {
        return OuraDataConverter()
    }

    override fun getUrlFormat(baseUrl: String?): String {
        return "$baseUrl/daily_sleep?start_date=%s&end_date=%s"
    }

    fun createRequest(url: String?): OuraRequest? {
        return OuraRequest(getUrlFormat(url), converter())
    }
}