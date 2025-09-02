package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraVO2Max
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraVO2MaxConverter(
    private val topic: String = "connect_oura_vo2_max",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val startTime = OffsetDateTime.parse(it["timestamp"].textValue())
                val startInstant = startTime.toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpoch(),
                    value = it.toVO2Max(startInstant),
                )
            }
    }

    private fun JsonNode.toVO2Max(
        startTime: Instant,
    ): OuraVO2Max {
        val data = this
        return OuraVO2Max.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            day = data.get("day")?.textValue()
            vo2Max = data.get("vo2_max")?.floatValue()
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyStressConverter::class.java)
    }
}
