/*
 * Copyright 2026 King's College London
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.googlehealth.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import kotlin.test.Test
import kotlin.test.assertEquals

/** Made-up users in the shape the authorizer's `users` endpoint returns. */
class GoogleHealthUsersTest {
    private val mapper = ObjectMapper().registerModule(JavaTimeModule())

    private fun user(id: String, serviceUserId: String?) = """
        {"id": "$id", "createdAt": "2026-10-01T00:00:00Z", "projectId": "p", "userId": "u$id",
         "sourceId": "s$id", "startDate": "2026-10-01T00:00:00Z", "sourceType": "GoogleHealth",
         "isAuthorized": true, "hasValidToken": true, "timesReset": 0,
         "serviceUserId": ${serviceUserId?.let { "\"$it\"" } ?: "null"}}
    """

    @Test
    fun incompleteUserIsSkippedNotTheWholeList() {
        val json = """{"users": [${user("1", "111")}, ${user("2", null)}, ${user("3", "333")}]}"""
        val users = mapper.readValue(json, GoogleHealthUsers::class.java).users
        assertEquals(listOf("1", "3"), users.map { it.id })
        assertEquals(listOf("111", "333"), users.map { it.serviceUserId })
        assertEquals("p", users.first().observationKey.projectId)
    }
}
