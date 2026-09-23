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
 * @author yatharthranjan
 */
class FieldValuesTest {
    private val mapper = ObjectMapper()

    @Test
    fun `parses typed-value array shape`() {
        val node = mapper.readTree(
            """
            [
              {"fieldName": "steps", "integerValue": 123},
              {"fieldName": "distance", "floatValue": 4.5},
              {"fieldName": "note", "stringValue": "hello"}
            ]
            """.trimIndent(),
        )
        val fields = FieldValues.from(node)

        assertEquals(123, fields.getInt("steps"))
        assertEquals(4.5, fields.getDouble("distance"))
        assertEquals("hello", fields.getString("note"))
        assertNull(fields.getInt("missing"))
    }

    @Test
    fun `parses flattened object shape`() {
        val node = mapper.readTree("""{"avg": 1.5, "max": 3, "min": null}""")
        val fields = FieldValues.from(node)

        assertEquals(1.5, fields.getDouble("avg"))
        assertEquals(3, fields.getInt("max"))
        assertNull(fields.getInt("min"))
    }

    @Test
    fun `falls back to later candidate keys and serializes list values`() {
        val node = mapper.readTree(
            """
            [
              {"fieldName": "heartRateVariabilityRmssd", "integerValue": 42},
              {"fieldName": "voltage_datas", "value": [1.5, -2.0]},
              {"fieldName": "custom", "doubleValue": 7.5}
            ]
            """.trimIndent(),
        )
        val fields = FieldValues.from(node)

        assertEquals(42, fields.getInt("heartRateVariabilityRMSSD", "heartRateVariabilityRmssd"))
        assertEquals("[1.5,-2.0]", fields.getString("voltage_datas"))
        assertNull(fields.getInt("voltage_datas"))
        assertEquals(7.5, fields.getDouble("custom"))
    }

    @Test
    fun `parses map-typed values`() {
        val node = mapper.readTree(
            """
            [
              {"fieldName": "plain", "mapValue": {"1": 10, "2": 20}},
              {"fieldName": "typed", "mapValue": {"1": {"integerValue": 5}}},
              {"fieldName": "entries", "mapValue": [{"key": "3", "value": {"integerValue": 7}}]}
            ]
            """.trimIndent(),
        )
        val fields = FieldValues.from(node)

        assertEquals(mapOf("1" to 10, "2" to 20), fields.getIntMap("plain"))
        assertEquals(mapOf("1" to 5), fields.getIntMap("typed"))
        assertEquals(mapOf("3" to 7), fields.getIntMap("entries"))
    }

    @Test
    fun `handles missing or null root node`() {
        val fields = FieldValues.from(null)

        assertNull(fields.getInt("anything"))
        assertNull(fields.getString("anything"))
    }
}
