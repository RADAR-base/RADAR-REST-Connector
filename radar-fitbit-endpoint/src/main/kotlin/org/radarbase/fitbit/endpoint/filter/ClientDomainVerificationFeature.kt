/*
 * Copyright (c) 2019. The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * See the file LICENSE in the root of this repository.
 */

package org.radarbase.fitbit.endpoint.filter

import jakarta.inject.Singleton
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.DynamicFeature
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.FeatureContext
import jakarta.ws.rs.ext.Provider

/** Authorization for different auth tags. */
@Provider
@Singleton
class ClientDomainVerificationFeature : DynamicFeature {
    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        val resourceMethod = resourceInfo.resourceMethod
        if (resourceMethod.isAnnotationPresent(ClientDomainVerification::class.java)) {
            context.register(ClientDomainVerificationFilter::class.java, Priorities.AUTHORIZATION)
        }
    }
}
