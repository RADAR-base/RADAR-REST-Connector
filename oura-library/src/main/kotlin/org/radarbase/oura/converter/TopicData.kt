package org.radarbase.oura.converter

import org.apache.avro.generic.IndexedRecord

/** Single value for a topic.  */
data class TopicData(
    val topic: String,
    val key: IndexedRecord,
    val value: IndexedRecord,
)
