package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraHeartRate
import org.radarcns.connector.oura.OuraHeartRateSource
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.io.IOException
import org.radarbase.oura.user.User

class OuraSessionHeartRateConverter(
    private val topic: String = "connect_oura_heart_rate",
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
        val startTime = OffsetDateTime.parse(this["start_datetime"].textValue())
        val startTimeEpoch = startTime.toInstant().toEpochMilli() / 1000.0
        val timeReceivedEpoch = System.currentTimeMillis() / 1000.0
        val id = this.get("id").textValue()
        val interval = this.get("heart_rate")?.get("interval")?.intValue() ?: throw IOException("Unable to get sample interval.")
        val items = this.get("heart_rate")?.get("items")
        if (items == null) return emptySequence()
        else {
            return items.asSequence()
                .mapIndexedCatching { index, value ->
                    TopicData(
                        key = user.observationKey,
                        topic = topic,
                        value = toHeartRate(
                            startTimeEpoch,
                            timeReceivedEpoch,
                            id,
                            index,
                            interval,
                            value.intValue()),
                    )
                }
        }
    }

    private fun toHeartRate(
        startTimeEpoch: Double,
        timeReceivedEpoch: Double,
        idString: String,
        index: Int,
        interval: Int,
        value: Int
    ): OuraHeartRate {
        val offset = interval * index
        return OuraHeartRate.newBuilder().apply {
            id = idString
            time = startTimeEpoch + offset
            timeReceived = timeReceivedEpoch
            bpm = value
            source = OuraHeartRateSource.SESSION
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSessionHeartRateConverter::class.java)
    }
}
