package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraHeartRateVariability
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime

class OuraSleepHrvConverter(
    private val topic: String = "connect_oura_heart_rate_variability",
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
                it.processSamples(user)
            }
    }

    private fun JsonNode.processSamples(
        user: User,
    ): Sequence<Result<TopicData>> {
        val startTime = OffsetDateTime.parse(this["bedtime_start"].textValue())
        val startTimeEpoch = startTime.toInstant().toEpochMilli() / 1000.0
        val timeReceivedEpoch = System.currentTimeMillis() / 1000.0
        val id = this.get("id").textValue()
        val interval = this.get("hrv")?.get("interval")?.intValue() ?: throw IOException("Unable to get sample interval.")
        val items = this.get("hrv")?.get("items")
        return if (items == null) {
            emptySequence()
        } else {
            items.asSequence()
                .mapIndexedCatching { index, value ->
                    TopicData(
                        key = user.observationKey,
                        topic = topic,
                        value = toHrv(
                            startTimeEpoch,
                            timeReceivedEpoch,
                            id,
                            index,
                            interval,
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
        index: Int,
        interval: Int,
        value: Float,
    ): OuraHeartRateVariability {
        val offset = interval * index
        return OuraHeartRateVariability.newBuilder().apply {
            id = idString
            time = startTimeEpoch + offset
            timeReceived = timeReceivedEpoch
            hrv = value
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSleepHrvConverter::class.java)
    }
}
