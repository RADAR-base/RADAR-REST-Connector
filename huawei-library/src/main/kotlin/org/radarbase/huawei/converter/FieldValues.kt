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

import com.fasterxml.jackson.databind.JsonNode

/**
 * Typed accessor for a single Huawei Health Kit sample point's field values.
 *
 * The Health Kit Data API (`sampleSet:polymerize`) represents each field of a sample point using
 * the same typed-value wrapper as the on-device HiHealth SDK's `Field`/`Value` model: a list of
 * objects shaped like `{"fieldName": "steps_delta", "integerValue": 123}` (or `floatValue`,
 * `longValue`, `stringValue` depending on the field's declared type). This class also tolerates a
 * flattened `{"fieldName": value, ...}` object, in case a particular endpoint or API version
 * returns the simplified shape, so a single parser can be reused across all sample-set based
 * routes.
 *
 * Field name constants follow Huawei's public `Field` identifiers (e.g. `steps_delta`, `calories`,
 * `avg`, `max`, `min`), as documented for the on-device and REST Health Kit APIs.
 *
 * @author yatharthranjan
 */
class FieldValues private constructor(private val values: Map<String, JsonNode>) {

    fun getInt(field: String): Int? = values[field]?.let { if (it.isNull) null else it.asInt() }

    fun getLong(field: String): Long? = values[field]?.let { if (it.isNull) null else it.asLong() }

    fun getDouble(field: String): Double? = values[field]?.let {
        if (it.isNull) null else it.asDouble()
    }

    fun getFloat(field: String): Float? = getDouble(field)?.toFloat()

    fun getString(field: String): String? = values[field]?.let {
        if (it.isNull) null else it.asText()
    }

    companion object {
        private const val FIELD_NAME_KEY = "fieldName"
        private val VALUE_KEYS =
            listOf("integerValue", "floatValue", "longValue", "stringValue", "value")

        fun from(node: JsonNode?): FieldValues {
            if (node == null || node.isMissingNode || node.isNull) {
                return FieldValues(emptyMap())
            }
            if (node.isArray) {
                val map = LinkedHashMap<String, JsonNode>()
                node.forEach { entry ->
                    val name = entry.get(FIELD_NAME_KEY)?.asText() ?: return@forEach
                    val value = VALUE_KEYS.firstNotNullOfOrNull { key -> entry.get(key) }
                    if (value != null) {
                        map[name] = value
                    }
                }
                return FieldValues(map)
            }
            if (node.isObject) {
                val map = LinkedHashMap<String, JsonNode>()
                node.properties().forEach { (name, value) -> map[name] = value }
                return FieldValues(map)
            }
            return FieldValues(emptyMap())
        }
    }
}
