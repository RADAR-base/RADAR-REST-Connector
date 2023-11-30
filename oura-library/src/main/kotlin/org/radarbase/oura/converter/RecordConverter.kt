package org.radarbase.oura.converter

import okhttp3.Headers
import org.radarbase.oura.request.RestRequest
import org.slf4j.LoggerFactory
import java.io.IOException

interface RecordConverter {
    @Throws(IOException::class)
    fun convert(
        request: RestRequest,
        headers: Headers,
        data: ByteArray,
    ): List<TopicData>

    companion object {
        var logger = LoggerFactory.getLogger(RecordConverter::class.java)
    }
}
