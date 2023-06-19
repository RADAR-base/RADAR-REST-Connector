package org.radarbase.oura.converter

import org.apache.avro.generic.IndexedRecord
import java.time.Instant

/** Single value for a topic.  */
data class TopicData(
        val topic: String,
        val key: IndexedRecord,
        val value: IndexedRecord,
)
