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

package org.radarbase.huawei.converter

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Every payload below is a snippet of a live Huawei Health Kit response.
 *
 * @author yatharthranjan
 */
class FieldValuesTest {
    private val mapper = ObjectMapper()

    @Test
    fun `parses typed-value array shape`() {
        // Sample point values from continuous_body_blood_pressure_statistics and
        // heart_rate_variability.
        val node = mapper.readTree(
            """
            [
              {"fieldName": "systolic_pressure_avg", "floatValue": 139.0},
              {"fieldName": "heartRateVariabilityRMSSD", "integerValue": 61}
            ]
            """.trimIndent(),
        )
        val fields = FieldValues.from(node)

        assertEquals(139.0, fields.getDouble("systolic_pressure_avg"))
        assertEquals(139, fields.getInt("systolic_pressure_avg"))
        assertEquals(61, fields.getInt("heartRateVariabilityRMSSD"))
        assertEquals("61", fields.getString("heartRateVariabilityRMSSD"))
        assertNull(fields.getInt("missing"))
    }

    @Test
    fun `parses flattened object shape`() {
        // activityRecords deviceInfo, with the device-specific uniqueId replaced.
        val node = mapper.readTree(
            """
            {"uniqueId": "device-unique-id", "devType": "Smart watch", "modelNum": "TBD",
             "manufacturer": "HUAWEI", "deviceCode": 308517120}
            """.trimIndent(),
        )
        val fields = FieldValues.from(node)

        assertEquals("HUAWEI", fields.getString("manufacturer"))
        assertEquals(308517120, fields.getInt("deviceCode"))
        assertEquals("Smart watch", fields.getString("devType"))
    }

    @Test
    fun `non-numeric text is null rather than zero for numeric getters`() {
        val fields = FieldValues.from(mapper.readTree("""{"devType": "Smart watch"}"""))

        assertNull(fields.getInt("devType"))
        assertNull(fields.getDouble("devType"))
    }

    @Test
    fun `serializes object-valued fields as JSON`() {
        // activityRecords activitySummary.paceSummary, trimmed.
        val node = mapper.readTree(
            """
            {"paceMap": {"1.0": 478.0, "2.0": 462.0}, "avgPace": 484.91802978515625,
             "bestPace": 322.58}
            """.trimIndent(),
        )
        val fields = FieldValues.from(node)

        assertEquals("""{"1.0":478.0,"2.0":462.0}""", fields.getString("paceMap"))
        assertEquals(484.91802978515625, fields.getDouble("avgPace"))
    }

    @Test
    fun `parses map-typed values`() {
        // continuous_exercise_intensity_v2_statistics sample point values.
        val node = mapper.readTree(
            """
            [
              {"fieldName": "totalIntensity", "integerValue": 32},
              {"fieldName": "intensity_map", "mapValue": [
                {"key": "1", "value": {"integerValue": 11}},
                {"key": "2", "value": {"integerValue": 15}}
              ]}
            ]
            """.trimIndent(),
        )
        val fields = FieldValues.from(node)

        assertEquals(mapOf("1" to 11, "2" to 15), fields.getIntMap("intensity_map"))
        assertEquals(32, fields.getInt("totalIntensity"))
    }

    @Test
    fun `handles missing or null root node`() {
        val fields = FieldValues.from(null)

        assertNull(fields.getInt("anything"))
        assertNull(fields.getString("anything"))
    }
}
