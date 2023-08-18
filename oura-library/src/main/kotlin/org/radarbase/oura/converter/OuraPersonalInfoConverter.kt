package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.oura.OuraPersonalInfo
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import org.radarbase.oura.user.User

class OuraPersonalInfoConverter(
    private val topic: String = "connect_oura_personal_info",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User
    ): Sequence<Result<TopicData>> {
        val array = root.optArray("data")
            ?: return emptySequence()
        return array.asSequence()
        .mapCatching { 
            val startTime = OffsetDateTime.parse(it["timestamp"].textValue())
            val startInstant = startTime.toInstant()
            TopicData(
                key = user.observationKey,
                topic = topic,
                value = it.toPersonalInfo(startInstant),
            )
        }
    }

    private fun JsonNode.toPersonalInfo(
        startTime: Instant,
    ): OuraPersonalInfo {
        return OuraPersonalInfo.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = optString("id")
            age = optInt("age")
            weight = optFloat("weight")
            height = optFloat("height")
            biologicalSex = optString("biological_sex")
            email = optString("email")
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraPersonalInfoConverter::class.java)
    }
}
