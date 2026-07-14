package org.radarbase.huawei.converter

import com.fasterxml.jackson.databind.JsonNode
import org.apache.avro.specific.SpecificRecord
import org.radarbase.huawei.user.User
import java.time.Instant

private fun JsonNode.epochInstant(field: String): Instant? {
    val value = this.get(field) ?: return null
    if (value.isNull) return null
    val millis = if (value.isTextual) value.asText().toLongOrNull() else value.asLong()
    return millis?.let { Instant.ofEpochMilli(it) }
}

/**
 * Generic converter for `GET /healthkit/v1/healthRecords` responses: iterates every record
 * returned for the requested `subDataTypeName` and builds one Avro record per entry via
 * [buildRecord].
 */
class HuaweiHealthRecordConverter(
    private val topic: String,
    private val buildRecord: (
        fields: FieldValues,
        startTime: Instant,
        endTime: Instant?,
        timeReceived: Instant,
    ) -> SpecificRecord,
) : HuaweiDataConverter {

    override fun processRecords(root: JsonNode, user: User): Sequence<Result<TopicData>> {
        val timeReceived = Instant.now()
        val records = root.get("healthRecords") ?: root.get("records") ?: return emptySequence()
        return records.asSequence()
            .mapCatching { record ->
                val startTime = record.epochInstant("startTime")
                    ?: error("Huawei health record is missing startTime")
                val endTime = record.epochInstant("endTime")
                val fieldValues = FieldValues.from(
                    record.get("value") ?: record.get("fieldValues") ?: record.get("field"),
                )
                TopicData(
                    topic = topic,
                    key = user.observationKey,
                    offset = startTime.epochSecond,
                    value = buildRecord(fieldValues, startTime, endTime, timeReceived),
                )
            }
    }
}
