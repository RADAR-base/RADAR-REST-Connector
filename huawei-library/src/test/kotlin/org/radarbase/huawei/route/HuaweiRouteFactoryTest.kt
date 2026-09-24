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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.avro.Schema
import org.radarbase.huawei.user.HuaweiUser
import org.radarbase.huawei.user.User
import org.radarbase.huawei.user.UserRepository
import org.radarcns.connector.huawei.HuaweiContinuousEcgDetail
import org.radarcns.connector.huawei.HuaweiHealthRecordDynamicBp
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises every [HuaweiRouteFactory] definition end to end: builds the route, feeds a
 * generously-populated fixture payload shaped like the endpoint it targets (`sampleSet:polymerize`,
 * `healthRecords`, or `activityRecords`), and asserts the converter produces exactly one record on
 * the definition's own topic without throwing. This is the main regression test against typos in
 * the ~90 hand-written Huawei field-value key strings (and the Avro builder calls around them).
 *
 * @author yatharthranjan
 */
class HuaweiRouteFactoryTest {
    private val mapper = ObjectMapper()
    private val fakeUser: User = HuaweiUser(
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
    private val fakeUserRepository = object : UserRepository {
        override fun get(key: String): User = fakeUser
        override fun stream(): Sequence<User> = sequenceOf(fakeUser)
        override fun getAccessToken(user: User): String = "token"
    }

    @Test
    fun `definitions have unique keys and topics`() {
        val keys = HuaweiRouteFactory.definitions.map { it.key }
        val topics = HuaweiRouteFactory.definitions.map { it.defaultTopic }

        assertEquals(keys.size, keys.toSet().size, "duplicate route definition keys: $keys")
        assertEquals(topics.size, topics.toSet().size, "duplicate route definition topics: $topics")
    }

    @Test
    fun `every definition converts a fixture payload without error`() {
        val failures = mutableListOf<String>()

        HuaweiRouteFactory.definitions.forEach { definition ->
            val route = definition.build(fakeUserRepository, definition.defaultTopic)
            try {
                val payload = fixtureFor(route)
                val records = route.converters.single().processRecords(payload, fakeUser).toList()
                val successes = records.mapNotNull { it.getOrNull() }

                if (successes.size != 1) {
                    failures += "${definition.key}: expected 1 record, got ${successes.size} " +
                        "(errors: ${records.mapNotNull { it.exceptionOrNull() }})"
                } else if (successes.first().topic != definition.defaultTopic) {
                    failures += "${definition.key}: unexpected topic ${successes.first().topic}"
                }
            } catch (e: Exception) {
                failures += "${definition.key}: threw $e"
            }
        }

        assertTrue(failures.isEmpty(), "Failures:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `ECG route reads record id, record fields and associated voltage data`() {
        val definition = HuaweiRouteFactory.definitions.single { it.key == "continuous_ecg_detail" }
        val route = definition.build(fakeUserRepository, definition.defaultTopic)
        val payload = mapper.readTree(
            """
            {"healthRecords": [{
              "startTime": $START_NANOS, "endTime": $END_NANOS,
              "dataTypeName": "com.huawei.continuous.ecg_record",
              "id": "ecg-1",
              "value": [
                {"fieldName": "ecg_type", "integerValue": 1},
                {"fieldName": "avg_heart_rate", "floatValue": 75.6},
                {"fieldName": "ecg_arrhythmia_type", "longValue": 8},
                {"fieldName": "user_symptom", "longValue": 1022},
                {"fieldName": "sampling_frequency", "integerValue": 500}
              ],
              "subDataDetails": [{
                "dataTypeName": "com.huawei.continuous.ecg_detail",
                "samplePoints": [
                  {"startTime": $START_NANOS, "value": [
                    {"fieldName": "voltage_datas", "stringValue": "0.1,0.2"}]}
                ]
              }]
            }]}
            """.trimIndent(),
        )

        val record = route.converters.single().processRecords(payload, fakeUser)
            .single().getOrThrow().value as HuaweiContinuousEcgDetail

        assertEquals("ecg-1", record.ecgRecordId)
        assertEquals(76, record.averageHeartRate)
        assertEquals(8, record.ecgArrhythmiaType)
        assertEquals("1022", record.userSymptom)
        assertEquals(500, record.samplingFrequency)
        assertEquals("0.1,0.2", record.voltageData)
    }

    private fun fixtureFor(route: HuaweiRoute) = when (route) {
        is HuaweiActivityRecordRoute -> activityRecordFixture()
        is HuaweiHealthRecordRoute -> healthRecordFixture()
        is HuaweiDailyPolymerizeRoute -> dailyPolymerizeFixture()
        is HuaweiSampleSetRoute -> sampleSetFixture()
        else -> error("Unknown route type: ${route::class}")
    }

    private fun sampleSetFixture(): ObjectNode {
        val root = mapper.createObjectNode()
        val sampleSet = root.putArray("sampleSet")
        val group = sampleSet.addObject()
        val samplePoints = group.putArray("samplePoints")
        val point = samplePoints.addObject()
        point.put("startTime", START_MILLIS)
        point.put("endTime", END_MILLIS)
        point.set<ArrayNode>("value", genericValueArray())
        return root
    }

    private fun dailyPolymerizeFixture(): ObjectNode {
        val root = mapper.createObjectNode()
        val groups = root.putArray("group")
        val group = groups.addObject()
        group.put("startTime", START_MILLIS)
        group.put("endTime", END_MILLIS)
        val sampleSet = group.putArray("sampleSet")
        val collector = sampleSet.addObject()
        val samplePoints = collector.putArray("samplePoints")
        val point = samplePoints.addObject()
        point.put("startTime", START_NANOS)
        point.put("endTime", END_NANOS)
        point.set<ArrayNode>("value", genericValueArray())
        return root
    }

    private fun healthRecordFixture(): ObjectNode {
        val root = mapper.createObjectNode()
        val records = root.putArray("healthRecords")
        val record = records.addObject()
        record.put("startTime", START_NANOS)
        record.put("endTime", END_NANOS)
        record.set<ArrayNode>("value", genericValueArray())
        return root
    }

    private fun activityRecordFixture(): ObjectNode {
        val root = mapper.createObjectNode()
        val records = root.putArray("activityRecord")
        val record = records.addObject()
        record.put("startTime", START_MILLIS)
        record.put("endTime", END_MILLIS)
        record.put("id", "activity-1")
        record.put("name", "Run")
        record.put("desc", "Morning run")
        record.put("timeZone", "Europe/London")
        record.put("activityType", "1")
        record.put("activeTime", 1000L)
        record.put("isKeepGoing", false)
        val device = record.putObject("device")
        device.put("manufacturer", "Huawei")
        device.put("type", 1)
        val summary = record.putObject("activitySummary")
        summary.put("avgPace", 300.0)
        summary.put("bestPace", 250.0)
        summary.putObject("paceMap")
        summary.putArray("dataSummary")
        summary.putArray("sectionSummary")
        return root
    }

    /** One value entry per literal field-value key used across [HuaweiRouteFactory], plus every
     * (snake-cased) field of [HuaweiHealthRecordDynamicBp] - covering both the ad hoc key names
     * used for most data types and the mechanically-derived ones used for the 24h ABPM record. */
    private fun genericValueArray(): ArrayNode {
        val array = mapper.createArrayNode()
        (LITERAL_FIELD_KEYS + dynamicBpFieldKeys()).distinct().forEach { key ->
            val entry = array.addObject()
            entry.put("fieldName", key)
            entry.put("integerValue", 1)
            entry.put("floatValue", 1.5)
            entry.put("stringValue", "test")
        }
        return array
    }

    private fun dynamicBpFieldKeys(): List<String> =
        (HuaweiHealthRecordDynamicBp::class.java.getField("SCHEMA$").get(null) as Schema).fields
            .map { it.name() }
            .filterNot { it in setOf("time", "timeReceived", "endTime") }

    companion object {
        private const val START_MILLIS = 1704067200000L // 2024-01-01T00:00:00Z
        private const val END_MILLIS = 1704070800000L // 2024-01-01T01:00:00Z
        private const val START_NANOS = START_MILLIS * 1_000_000L
        private const val END_NANOS = END_MILLIS * 1_000_000L

        private val LITERAL_FIELD_KEYS = listOf(
            "activeCalories", "activeCaloriesGoal", "activeHours", "activeHoursGoal",
            "activity_type", "all_sleep_time", "ascent_total", "avg", "avg_body_fat_rate",
            "avg_heart_rate", "avgBreatheRate", "awake_time", "calories", "calories_total",
            "correlate_mealtime", "correlate_sleep", "count", "deep_sleep_part", "deep_sleep_time",
            "descent_total", "diastolic_pressure_avg", "diastolic_pressure_max",
            "diastolic_pressure_min", "distance", "distance_delta", "dream_time", "duration",
            "ecg_arrhythmia_type", "emotionStatus", "eventName", "exercise_type", "exerciseTime",
            "exerciseTimeGoal", "extendData", "fall_asleep_time", "fragments", "go_bed_time",
            "heartRateVariabilityRMSSD", "highBodyTemperatureAlarm", "intensity", "isActive",
            "last", "level", "light_sleep_time", "max", "max_body_fat_rate", "max_heart_rate",
            "maxBreatheRate", "maxBreathrateBaseline", "maxSpO2", "meal", "measure_count", "min",
            "min_body_fat_rate", "min_heart_rate", "minBreatheRate", "minBreathrateBaseline",
            "minSpO2", "off_bed_time", "onOffBedState", "predictedCalories", "prepare_sleep_time",
            "recordDay", "remarks", "sample_source", "sampling_frequency", "saturation_avg",
            "saturation_last", "saturation_max", "saturation_min", "sleep_efficiency",
            "sleep_latency", "sleep_score", "sleep_state", "sleep_type", "span", "sphygmus_avg",
            "sphygmus_last", "sphygmus_max", "sphygmus_min", "status", "steps", "steps_delta",
            "stepsGoal", "subStatus", "systolic_pressure_avg", "systolic_pressure_max",
            "systolic_pressure_min", "threshold", "timeZone", "totalCalories", "type",
            "user_symptom", "value", "vo2max", "voltage_datas", "wakeup_count", "wakeup_time",
        )
    }
}
