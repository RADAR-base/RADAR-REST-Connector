package org.radarbase.dexcom.converter

import com.fasterxml.jackson.databind.JsonNode
import okhttp3.Headers
import org.radarbase.dexcom.request.DexcomRequestGenerator.Companion.JSON_READER
import org.radarbase.dexcom.request.RestRequest
import org.radarbase.dexcom.user.User
import java.time.Instant

interface DexcomDataConverter : RecordConverter {
    fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>>

    override fun convert(
        request: RestRequest,
        headers: Headers,
        data: ByteArray,
    ): List<TopicData> {
        val node = JSON_READER.readTree(data)

        return processRecords(node, request.user)
            .mapNotNull { result ->
                result.fold(
                    { it },
                    {
                        RecordConverter.logger.error("Data conversion failed: ${it.message}")
                        null
                    },
                )
            }
            .toList()
    }

    fun Instant.toEpoch(): Long = toEpochMilli() / 1000
}
