package org.radarbase.oura.converter

import org.apache.avro.generic.IndexedRecord
import org.apache.avro.specific.SpecificRecord

/** Single value for a topic.  */
data class TopicData(
    val topic: String,
    val key: SpecificRecord,
    val value: SpecificRecord,
)