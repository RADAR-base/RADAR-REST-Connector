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
package org.radarbase.fitbit.endpoint.resource

import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import org.radarbase.fitbit.endpoint.config.FitbitEndpointConfig
import org.radarbase.fitbit.endpoint.filter.ClientDomainVerification
import org.radarbase.fitbit.endpoint.service.FitbitVerificationService
import org.radarbase.fitbit.endpoint.service.NotificationService

@Path("webhook")
class WebhookResource(
    @Context private val fitbitVerificationService: FitbitVerificationService,
    @Context private val notificationService: NotificationService,
) {
    @GET
    @ClientDomainVerification("fitbit.com")
    fun verifyCode(
        @Context config: FitbitEndpointConfig,
        @PathParam("verify") verificationCode: String?,
    ): Response {
        return if (verificationCode == config.fitbit.verificationCode) {
            Response.noContent().build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @POST
    @ClientDomainVerification("fitbit.com")
    fun submitNotification(
        @HeaderParam("X-Fitbit-Signature") fitbitSignature: String?,
        contents: String,
    ): Response {
        if (!fitbitVerificationService.isSignatureValid(fitbitSignature, contents)) {
            return Response.status(Response.Status.NOT_FOUND).build()
        }

        notificationService.add(contents)

        return Response.noContent().build()
    }
}
