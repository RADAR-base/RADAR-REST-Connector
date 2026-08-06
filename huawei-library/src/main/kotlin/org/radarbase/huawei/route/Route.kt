/*
 * Copyright 2026 Onsentia
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

package org.radarbase.huawei.route

import org.radarbase.huawei.request.RestRequest
import org.radarbase.huawei.user.User
import java.time.Duration
import java.time.Instant

/**
 * @author yatharthranjan
 */
interface Route {

    fun generateRequests(user: User, start: Instant, end: Instant): Sequence<RestRequest>

    fun generateRequests(user: User, start: Instant, end: Instant, max: Int): Sequence<RestRequest>

    /**
     * This is how it would appear in the offsets
     */
    override fun toString(): String

    /**
     * The duration of data to request in a single request of this route.
     */
    val maxIntervalPerRequest: Duration
}
