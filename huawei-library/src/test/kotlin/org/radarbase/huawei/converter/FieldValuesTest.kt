package org.radarbase.huawei.converter

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
