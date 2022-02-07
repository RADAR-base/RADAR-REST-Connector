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
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import jakarta.ws.rs.core.Response
import org.radarbase.fitbit.endpoint.api.NotificationFilter
import org.radarbase.fitbit.endpoint.api.NotificationSelection
import org.radarbase.fitbit.endpoint.service.NotificationService
import org.radarbase.jersey.auth.Authenticated
import java.net.URI

@Path("notifications")
@Authenticated
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
class NotificationResource(
    @Context private val notificationService: NotificationService,
) {
    @POST
    fun query(
        filter: NotificationFilter,
    ): Response {
        val selected = notificationService.makeSelection(filter)
        return Response.created(URI.create("notifications/${selected.id}"))
            .entity(selected)
            .build()
    }

    @GET
    @Path("/{selectionId}")
    fun notifications(
        @PathParam("selectionId") selectionId: String,
    ): NotificationSelection = notificationService.getSelection(selectionId)

    @DELETE
    @Path("/{selectionId}")
    fun deleteNotifications(
        @PathParam("selectionId") selectionId: String,
    ): Response {
        notificationService.deleteSelection(selectionId)
        return Response.noContent().build()
    }
}
