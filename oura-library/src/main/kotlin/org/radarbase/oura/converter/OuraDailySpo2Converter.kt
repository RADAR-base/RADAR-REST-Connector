package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraDailySpo2
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class OuraDailySpo2Converter(
    private val topic: String = "connect_oura_daily_spo2",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val startTime = OffsetDateTime.parse(
                    it["day"].textValue(),
                    DateTimeFormatter.ISO_LOCAL_DATE,
                )
                val startInstant = startTime.toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpochMilli(),
                    value = it.toDailySpo2(startInstant),
                )
            }
    }

    private fun JsonNode.toDailySpo2(
        startTime: Instant,
    ): OuraDailySpo2 {
        val data = this
        return OuraDailySpo2.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id").textValue()
            spo2AveragePercentage = data.get("spo2_percentage")?.get("average")?.floatValue()
            day = data.get("day").textValue()
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailySpo2Converter::class.java)
    }
}
