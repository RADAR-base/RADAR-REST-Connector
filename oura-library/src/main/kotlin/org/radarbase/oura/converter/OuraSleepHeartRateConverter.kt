package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraHeartRate
import org.radarcns.connector.oura.OuraHeartRateSource
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime

class OuraSleepHeartRateConverter(
    private val topic: String = "connect_oura_heart_rate",
    private val sampleKey: String = "heart_rate",
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
        val startTime = OffsetDateTime.parse(this["bedtime_start"].textValue())
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
                        value = toHeartRate(
                            time,
                            timeReceivedEpoch,
                            id,
                            value.intValue(),
                        ),
                    )
                }
        }
    }

    private fun toHeartRate(
        startTimeEpoch: Double,
        timeReceivedEpoch: Double,
        idString: String,
        value: Int,
    ): OuraHeartRate {
        return OuraHeartRate.newBuilder().apply {
            id = idString
            time = startTimeEpoch
            timeReceived = timeReceivedEpoch
            bpm = value
            source = OuraHeartRateSource.SLEEP
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSleepHeartRateConverter::class.java)
    }
}
