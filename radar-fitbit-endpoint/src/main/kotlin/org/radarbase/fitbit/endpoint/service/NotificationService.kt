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

package org.radarbase.fitbit.endpoint.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.core.Context
import org.radarbase.fitbit.endpoint.api.FitbitNotification
import org.radarbase.fitbit.endpoint.api.NotificationFilter
import org.radarbase.fitbit.endpoint.api.NotificationSelection

class NotificationService(
    @Context objectMapper: ObjectMapper
) {
    private val contentReader = objectMapper.readerFor(object : TypeReference<List<FitbitNotification>>() {})

    fun add(contents: String) {
        val notifications = contentReader.readValue<List<FitbitNotification>>(contents)

        TODO("Add notifications to redis")
    }

    fun makeSelection(filter: NotificationFilter): NotificationSelection {
        TODO("Retrieve notifications from redis and mark them for being currently processing")
    }

    fun getSelection(selectionId: String): NotificationSelection {
        TODO("Retrieve selection by ID")
    }

    fun deleteSelection(selectionId: String) {
        TODO("Delete selection and associated notifications by ID")
    }
}
