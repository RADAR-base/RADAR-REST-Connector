/*
 * Copyright 2018 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.radarbase.fitbit.endpoint.filter

import jakarta.inject.Provider
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import org.glassfish.grizzly.http.server.Request
import org.slf4j.LoggerFactory
import java.net.InetAddress

class ClientDomainVerificationFilter(
    /**
     * Check that the token has given permissions.
     */
    @Context private val resourceInfo: ResourceInfo,
    @Context private val req: Provider<Request>
) : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val annotation = resourceInfo.resourceMethod.getAnnotation(ClientDomainVerification::class.java)

        val ipAddress = requestContext.getHeaderString("X-Forwarded-For")
            ?: req.get().remoteAddr

        val remoteHostName = InetAddress.getByName(ipAddress).hostName
        if (remoteHostName != annotation.domainName && !remoteHostName.endsWith(".${annotation.domainName}")) {
            logger.error("Failed to verify that IP address {} belongs to domain name {}. It resolves to {} instead.", ipAddress, annotation.domainName, remoteHostName)
            requestContext.abortWith(Response.status(Response.Status.NOT_FOUND).build())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClientDomainVerificationFilter::class.java.name)
    }
}
