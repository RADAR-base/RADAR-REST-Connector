package org.radarbase.oura.converter

import okhttp3.Headers
import org.radarbase.oura.request.RestRequest
import org.apache.avro.specific.SpecificRecord
import java.io.IOException
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory

interface RecordConverter {
    @Throws(IOException::class)
    fun convert(
        request: RestRequest,
        headers: Headers,
        data: ByteArray,
    ): List<Pair<SpecificRecord, SpecificRecord>>

    companion object {
        fun nearFuture(): Instant {
            return Instant.now().plus(NEAR_FUTURE)
        }
        var logger = LoggerFactory.getLogger(RecordConverter::class.java)
        val MIN_INSTANT = Instant.EPOCH
        const val TIMESTAMP_OFFSET_KEY = "timestamp"
        private val NEAR_FUTURE = Duration.ofDays(31L)
    }
}
