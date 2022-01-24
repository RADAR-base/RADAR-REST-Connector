package org.radarbase.fitbit.endpoint.inject

import org.radarbase.fitbit.endpoint.config.FitbitEndpointConfig
import org.radarbase.jersey.enhancer.EnhancerFactory
import org.radarbase.jersey.enhancer.Enhancers
import org.radarbase.jersey.enhancer.JerseyResourceEnhancer

/** This binder needs to register all non-Jersey classes, otherwise initialization fails. */
class ManagementPortalEnhancerFactory(private val config: FitbitEndpointConfig) : EnhancerFactory {
    override fun createEnhancers(): List<JerseyResourceEnhancer> {
        return listOf(
            GatewayResourceEnhancer(config),
            Enhancers.radar(config.auth),
            Enhancers.managementPortal(config.auth),
            Enhancers.health,
            Enhancers.exception,
        )
    }
}
