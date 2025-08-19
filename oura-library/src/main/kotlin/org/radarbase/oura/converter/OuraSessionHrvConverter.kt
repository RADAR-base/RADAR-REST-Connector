package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraHeartRateVariability
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime

class OuraSessionHrvConverter(
    private val topic: String = "connect_oura_heart_rate_variability",
    private val sampleKey: String = "heart_rate_variability",
) : OuraDataConverter {

    @Throws(IOException::class)
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .flatMap {
                runCatching {
                    it.processSamples(user)
                }.getOrElse {
                    logger.error("Error processing records", it.message)
                    emptySequence()
                }
            }
    }

    private fun JsonNode.processSamples(
        user: User,
    ): Sequence<Result<TopicData>> {
        val startTime = OffsetDateTime.parse(this["start_datetime"].textValue())
        val startTimeEpoch = startTime.toInstant().toEpochMilli() / 1000.0
        val timeReceivedEpoch = System.currentTimeMillis() / 1000.0
        val id = this.get("id").textValue()
        val interval = this.get(sampleKey)?.get("interval")?.intValue()
        val items = this.get(sampleKey)?.get("items")
        return if (items == null || interval == null) {
            emptySequence()
        } else {
            items.asSequence()
                .mapIndexedCatching { index, value ->
                    val offset = index * interval
                    val time = startTimeEpoch + offset
                    TopicData(
                        key = user.observationKey,
                        topic = topic,
                        offset = time.toLong(),
                        value = toHrv(
                            time,
                            timeReceivedEpoch,
                            id,
                            value.floatValue(),
                        ),
                    )
                }
        }
    }

    private fun toHrv(
        startTimeEpoch: Double,
        timeReceivedEpoch: Double,
        idString: String,
        value: Float,
    ): OuraHeartRateVariability {
        return OuraHeartRateVariability.newBuilder().apply {
            id = idString
            time = startTimeEpoch
            timeReceived = timeReceivedEpoch
            hrv = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSessionHrvConverter::class.java)
    }
}
