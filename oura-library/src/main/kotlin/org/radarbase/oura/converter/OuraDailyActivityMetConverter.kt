package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraMet
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.io.IOException
import org.radarbase.oura.user.User

class OuraDailyActivityMetConverter(
    private val topic: String = "connect_oura_met",
) : OuraDataConverter {

    @Throws(IOException::class)
    override fun processRecords(
        root: JsonNode,
        user: User
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
        .flatMap { 
            it.processSamples(user)
        }
    }

    private fun JsonNode.processSamples(
        user: User
    ): Sequence<Result<TopicData>> {
        val startTime = OffsetDateTime.parse(this["timestamp"].textValue())
        val startTimeEpoch = startTime.toInstant().toEpochMilli() / 1000.0
        val timeReceivedEpoch = System.currentTimeMillis() / 1000.0
        val id = this.get("id").textValue()
        val interval = this.get("met")?.get("interval")?.intValue() ?: throw IOException("Unable to get sample interval.")
        val items = this.get("met")?.get("items")
        return if (items == null) {
            emptySequence()
        } else {
            items.asSequence()
                .mapIndexedCatching { index, value ->
                    TopicData(
                        key = user.observationKey,
                        topic = topic,
                        value = toMet(
                            startTimeEpoch,
                            timeReceivedEpoch,
                            id,
                            index,
                            interval,
                            value.floatValue()),
                    )
                }
        }
    }

    private fun toMet(
        startTimeEpoch: Double,
        timeReceivedEpoch: Double,
        idString: String,
        index: Int,
        interval: Int,
        value: Float
    ): OuraMet {
        val offset = interval * index
        return OuraMet.newBuilder().apply {
            id = idString
            time = startTimeEpoch + offset
            timeReceived = timeReceivedEpoch
            met = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyActivityMetConverter::class.java)
    }
}
