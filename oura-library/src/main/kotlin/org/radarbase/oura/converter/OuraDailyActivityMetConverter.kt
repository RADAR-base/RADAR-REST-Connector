package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraMet
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime

class OuraDailyActivityMetConverter(
    private val topic: String = "connect_oura_met",
    private val sampleKey: String = "met",
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
        val startTime = OffsetDateTime.parse(this["timestamp"].textValue())
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
                    val offset = interval * index
                    val time = startTimeEpoch + offset
                    TopicData(
                        key = user.observationKey,
                        topic = topic,
                        offset = time.toLong(),
                        value = toMet(
                            time,
                            timeReceivedEpoch,
                            id,
                            value.floatValue(),
                        ),
                    )
                }
        }
    }

    private fun toMet(
        startTimeEpoch: Double,
        timeReceivedEpoch: Double,
        idString: String,
        value: Float,
    ): OuraMet {
        return OuraMet.newBuilder().apply {
            id = idString
            time = startTimeEpoch
            timeReceived = timeReceivedEpoch
            met = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyActivityMetConverter::class.java)
    }
}
