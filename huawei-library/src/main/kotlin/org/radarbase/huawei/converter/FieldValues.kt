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

    /** Huawei's own ID of the record these fields belong to, where the endpoint returns one
     * (e.g. `healthRecords`). */
    var recordId: String? = null
        private set

    /** Field values of the associated detail sample points of a health record (its
     * `subDataDetails`), in response order, when they were requested and returned. */
    var subData: List<FieldValues> = emptyList()
        private set

    /** Copy of these field values with the given record-level context attached. */
    fun withRecord(recordId: String?, subData: List<FieldValues>): FieldValues =
        FieldValues(values).also {
            it.recordId = recordId
            it.subData = subData
        }

    /*
     * Every accessor accepts one or more candidate keys and returns the first one present, so a
     * field whose casing Huawei's docs don't pin down unambiguously can list both spellings.
     */

    fun getInt(vararg fields: String): Int? = number(fields)?.let {
        if (it.isNumber) it.asInt() else it.asText().trim().toIntOrNull()
    }

    fun getLong(vararg fields: String): Long? = number(fields)?.let {
        if (it.isNumber) it.asLong() else it.asText().trim().toLongOrNull()
    }

    fun getDouble(vararg fields: String): Double? = number(fields)?.let {
        if (it.isNumber) it.asDouble() else it.asText().trim().toDoubleOrNull()
    }

    fun getFloat(vararg fields: String): Float? = getDouble(*fields)?.toFloat()

    /** Textual fields are returned as-is; array/object-valued fields (e.g. the ECG voltage sample
     * list) are returned as their JSON serialization rather than Jackson's empty `asText()`. */
    fun getString(vararg fields: String): String? = lookup(fields)?.let {
        if (it.isContainerNode) it.toString() else it.asText()
    }

    /**
     * Reads a `Map<Integer, Integer>`-typed field (e.g. Huawei's exercise-type-to-duration map),
     * as an object whose keys are stringified (Avro maps require string keys). Best-effort: the
     * exact wire shape of a Huawei map-typed field is not confirmed against a live API response
     * (Huawei's typed-value array uses `integerValue`/`floatValue`/`stringValue`/`longValue` for
     * scalars, so `mapValue` is assumed by the same `<type>Value` convention). Accepts a plain
     * `{"key": 1}` object, an object of typed values `{"key": {"integerValue": 1}}`, or an array of
     * `{"key": ..., "value": ...}` entries.
     */
    fun getIntMap(vararg fields: String): Map<String, Int>? {
        val node = lookup(fields) ?: return null
        return when {
            node.isObject -> node.properties().mapNotNull { (key, value) ->
                unwrap(value)?.let { key to it.asInt() }
            }.toMap()
            node.isArray -> node.mapNotNull { entry ->
                val key = entry.get("key")?.takeUnless { it.isNull }?.asText()
                    ?: return@mapNotNull null
                unwrap(entry.get("value"))?.let { key to it.asInt() }
            }.toMap()
            else -> null
        }
    }

    private fun lookup(fields: Array<out String>): JsonNode? =
        fields.firstNotNullOfOrNull { field -> values[field]?.takeUnless { it.isNull } }

    /** Numeric or textual node; Jackson's `asInt()` etc. would turn anything else (and
     * non-numeric text) into 0 rather than null. */
    private fun number(fields: Array<out String>): JsonNode? =
        lookup(fields)?.takeIf { it.isNumber || it.isTextual }

    companion object {
        private const val FIELD_NAME_KEY = "fieldName"
        private val VALUE_KEYS =
            listOf("integerValue", "floatValue", "longValue", "stringValue", "mapValue", "value")

        /** Unwraps a typed-value wrapper (`{"integerValue": 1}`) to its value; returns other
         * nodes unchanged. */
        private fun unwrap(node: JsonNode?): JsonNode? {
            if (node == null || node.isNull || node.isMissingNode) return null
            if (!node.isObject) return node
            return VALUE_KEYS.firstNotNullOfOrNull { key -> node.get(key) } ?: node
        }

        fun from(node: JsonNode?): FieldValues {
            if (node == null || node.isMissingNode || node.isNull) {
                return FieldValues(emptyMap())
            }
            if (node.isArray) {
                val map = LinkedHashMap<String, JsonNode>()
                node.forEach { entry ->
                    val name = entry.get(FIELD_NAME_KEY)?.asText() ?: return@forEach
                    // Prefer the known typed-value keys; fall back to whatever other single
                    // property the entry carries, in case Huawei uses a type name not listed here.
                    val value = VALUE_KEYS.firstNotNullOfOrNull { key -> entry.get(key) }
                        ?: entry.properties().firstOrNull { (key, _) -> key != FIELD_NAME_KEY }
                            ?.value
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
