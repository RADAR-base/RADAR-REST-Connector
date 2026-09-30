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
package org.radarbase.huawei.request

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.radarbase.huawei.offset.Offset
import org.radarbase.huawei.route.HuaweiDailyPolymerizeRoute
import org.radarbase.huawei.route.HuaweiRoute
import org.radarbase.huawei.route.HuaweiSampleSetRoute
import org.radarbase.huawei.route.Route
import org.radarbase.huawei.user.HuaweiUser
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserNotAuthorizedException
import org.radarbase.huawei.user.UserRepository
import org.radarcns.connector.huawei.HuaweiContinuousStepsDelta
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author yatharthranjan
 */
class HuaweiRequestGeneratorTest {
    private val user: User = HuaweiUser(
        id = "u1",
        createdAt = Instant.now(),
        projectId = "p",
        userId = "u",
        humanReadableUserId = null,
        sourceId = "s",
        externalId = "ext",
        isAuthorized = true,
        startDate = Instant.parse("2024-01-01T00:00:00Z"),
    )

    private var tokenError: Exception? = null
    private var invalidated = 0
    private val repository = object : UserRepository {
        override fun get(key: String): User = user
        override fun stream(): Sequence<User> = sequenceOf(user)
        override fun getAccessToken(user: User): String = tokenError?.let { throw it } ?: "token"
        override fun invalidateAccessToken(user: User) {
            invalidated++
        }
    }

    private val offsets = mutableMapOf<String, Instant>()
    private val offsetManager = object : HuaweiOffsetManager {
        override fun getOffset(route: Route, user: User): Offset? =
            offsets[route.toString()]?.let { Offset(user, route, it) }

        override fun updateOffsets(route: Route, user: User, offset: Instant) {
            offsets[route.toString()] = offset
        }
    }

    private val route = HuaweiSampleSetRoute(
        repository,
        "com.huawei.continuous.steps.delta",
        "topic",
    ) { f, start, _, received ->
        HuaweiContinuousStepsDelta.newBuilder().apply {
            time = start.epochSecond.toDouble()
            timeReceived = received.epochSecond.toDouble()
            stepsDelta = f.getInt("steps_delta")
        }.build()
    }

    private val generator = HuaweiRequestGenerator(repository, offsetManager, listOf(route))

    private fun request(route: HuaweiRoute, start: Instant, end: Instant) = RestRequest(
        Request.Builder().url("https://example.com").build(),
        user,
        route,
        start,
        end,
    )

    private fun response(req: RestRequest, code: Int, body: String) = Response.Builder()
        .request(req.request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("msg")
        .body(body.toResponseBody())
        .build()

    private fun pointsBody(vararg starts: Instant, hasMoreData: Boolean = false): String {
        val points = starts.joinToString(",") { start ->
            """{"startTime": ${start.toEpochMilli() * 1_000_000},
               "value": [{"fieldName": "steps_delta", "integerValue": 1}]}"""
        }
        return """{"hasMoreData": $hasMoreData,
                   "group": [{"sampleSet": [{"samplePoints": [$points]}]}]}"""
    }

    @Test
    fun `empty historic window advances to its end`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = Instant.parse("2024-01-31T00:00:00Z")
        val req = request(route, start, end)

        val records = generator.requestSuccessful(req, response(req, 200, pointsBody()))

        assertTrue(records.isEmpty())
        assertEquals(end, offsets[route.toString()])
    }

    @Test
    fun `empty recent window does not advance past the late sync window`() {
        val now = Instant.now()
        val start = now.minus(Duration.ofDays(20))
        offsets[route.toString()] = start
        val req = request(route, start, now)

        generator.requestSuccessful(req, response(req, 200, pointsBody()))

        val offset = offsets.getValue(route.toString())
        val limit = Instant.now().minus(Duration.ofDays(7))
        assertTrue(offset <= limit, "offset $offset advanced too far")
        assertTrue(offset > now.minus(Duration.ofDays(8)))
    }

    @Test
    fun `recent records advance to just after the latest record and drop already-seen ones`() {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val offset = now.minus(Duration.ofHours(2))
        offsets[route.toString()] = offset
        val req = request(route, offset, now)
        val seen = offset.minusSeconds(30)
        val latest = now.minus(Duration.ofHours(1))

        val records = generator.requestSuccessful(
            req,
            response(req, 200, pointsBody(seen, offset, latest)),
        )

        assertEquals(listOf(offset.epochSecond, latest.epochSecond), records.map { it.offset })
        assertEquals(latest.plusSeconds(1), offsets[route.toString()])
    }

    @Test
    fun `partial historic response continues after the latest record`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val latest = Instant.parse("2024-01-05T00:00:00Z")
        val req = request(route, start, Instant.parse("2024-01-31T00:00:00Z"))

        generator.requestSuccessful(req, response(req, 200, pointsBody(latest, hasMoreData = true)))

        assertEquals(latest.plusSeconds(1), offsets[route.toString()])
    }

    @Test
    fun `failed chunk stops the remaining chunks of that route`() {
        val requests = generator.requests(route, user, 100).iterator()
        val first = requests.next()
        generator.handleResponse(first, response(first, 500, "boom"))

        assertTrue(!requests.hasNext())
        assertEquals(null, offsets[route.toString()])
    }

    @Test
    fun `unauthorized user is backed off instead of throwing`() {
        tokenError = UserNotAuthorizedException("revoked")

        assertTrue(generator.requests(route, user, 100).toList().isEmpty())

        tokenError = null
        assertTrue(generator.requests(route, user, 100).toList().isEmpty(), "not backed off")
    }

    @Test
    fun `no data collector error is treated as an empty response`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = Instant.parse("2024-01-31T00:00:00Z")
        val req = request(route, start, end)
        val body = """{"error":{"code":400,"message":"no default dataCollector found for: x."}}"""

        val result = generator.handleResponse(req, response(req, 400, body))

        assertTrue(result is HuaweiResult.Success)
        assertEquals(end, offsets[route.toString()])
    }

    @Test
    fun `401 invalidates the cached access token`() {
        val req = request(route, user.startDate, user.startDate.plus(Duration.ofDays(1)))

        generator.handleResponse(req, response(req, 401, "{}"))

        assertEquals(1, invalidated)
    }

    @Test
    fun `daily requests cover whole completed UTC days only`() {
        val daily = HuaweiDailyPolymerizeRoute(
            repository,
            "com.huawei.continuous.steps.delta",
            "daily",
        ) { _, start, _, received ->
            HuaweiContinuousStepsDelta.newBuilder().apply {
                time = start.epochSecond.toDouble()
                timeReceived = received.epochSecond.toDouble()
            }.build()
        }
        val start = Instant.parse("2024-01-01T10:00:00Z")
        val end = Instant.parse("2024-01-04T10:00:00Z")

        val requests = daily.generateRequests(user, start, end, 10).toList()

        assertEquals(1, requests.size)
        assertEquals(Instant.parse("2024-01-02T00:00:00Z"), requests[0].startDate)
        assertEquals(Instant.parse("2024-01-04T00:00:00Z"), requests[0].endDate)
        val body = okio.Buffer().also { requests[0].request.body!!.writeTo(it) }.readUtf8()
        assertTrue(body.contains("\"startDay\":\"20240102\""), body)
        assertTrue(body.contains("\"endDay\":\"20240103\""), body)
    }
}
