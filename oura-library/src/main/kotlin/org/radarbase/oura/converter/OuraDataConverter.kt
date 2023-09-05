package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import okhttp3.Headers
import org.radarbase.oura.request.OuraRequestGenerator.Companion.JSON_READER
import org.radarbase.oura.request.RestRequest
import org.radarbase.oura.user.User
import org.apache.avro.specific.SpecificRecord
/**
 * Abstract class to help convert Fitbit data to Avro Data.
 */
interface OuraDataConverter : RecordConverter {
    /** Process the JSON records generated by given request.  */
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

        return this.processRecords(node, request.user)
        .mapNotNull { r -> r.fold(
            {
                it
            },
            {
                logger.error("Data conversion failed..")
                null
            })
        }
        .toList()
    }
}
