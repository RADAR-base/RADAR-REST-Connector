package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraRestModePeriod
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraRestModePeriodConverter(
    private val topic: String = "connect_oura_rest_mode_period",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val startTime = OffsetDateTime.parse(it["start_time"].textValue())
                val startInstant = startTime.toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpochMilli(),
                    value = it.toRestModePeriod(startInstant),
                )
            }
    }

    private fun JsonNode.toRestModePeriod(
        startTime: Instant,
    ): OuraRestModePeriod {
        val data = this
        return OuraRestModePeriod.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            endTime = OffsetDateTime.parse(data.get("end_time").textValue()).toInstant().toEpochMilli() / 1000.0
            id = data.get("id").textValue()
            startDay = data.get("start_day").textValue()
            endDay = data.get("end_day").textValue()
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraRestModePeriodConverter::class.java)
    }
}
