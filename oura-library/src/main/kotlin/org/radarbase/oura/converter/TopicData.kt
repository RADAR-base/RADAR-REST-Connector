package org.radarbase.oura.converter

import org.apache.avro.generic.IndexedRecord
import java.time.Instant

/** Single value for a topic.  */
data class TopicData(
        var sourceOffset: Instant,
        val topic: String,
        val value: IndexedRecord,
)