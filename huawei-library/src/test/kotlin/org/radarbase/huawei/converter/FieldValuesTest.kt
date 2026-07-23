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
    fun `handles missing or null root node`() {
        val fields = FieldValues.from(null)

        assertNull(fields.getInt("anything"))
        assertNull(fields.getString("anything"))
    }
}
