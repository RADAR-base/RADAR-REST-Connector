package org.radarbase.fitbit.endpoint.inject

import org.glassfish.jersey.internal.inject.AbstractBinder
import org.radarbase.fitbit.endpoint.config.FitbitEndpointConfig
import org.radarbase.jersey.enhancer.JerseyResourceEnhancer
import org.radarbase.jersey.filter.Filters

class GatewayResourceEnhancer(private val config: FitbitEndpointConfig) : JerseyResourceEnhancer {
    override val packages: Array<String> = arrayOf(
        "org.radarbase.fitbit.endpoint.filter",
        "org.radarbase.fitbit.endpoint.resource",
    )

    override val classes: Array<Class<*>> = arrayOf(
        Filters.logResponse,
    )

    override fun AbstractBinder.enhance() {
        bind(config)
            .to(FitbitEndpointConfig::class.java)
    }
}
