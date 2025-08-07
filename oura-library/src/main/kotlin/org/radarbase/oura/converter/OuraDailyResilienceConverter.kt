package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraDailyResilience
import org.radarcns.connector.oura.OuraResilienceLevel
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraDailyResilienceConverter(
    private val topic: String = "connect_oura_daily_resilience",
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
                    value = it.toDailyResilience(startInstant),
                )
            }
    }

    private fun JsonNode.toDailyResilience(
        startTime: Instant,
    ): OuraDailyResilience {
        val data = this
        return OuraDailyResilience.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            day = data.get("day")?.textValue()
            contributorSleepRecovery = data.get("contributors")?.get("sleep_recovery")?.floatValue()
            contributorDaytimeRecovery = data.get("contributors")?.get("daytime_recovery")?.floatValue()
            contributorStress = data.get("contributors")?.get("stress")?.floatValue()
            level = data.get("level")?.textValue()?.classifyResilienceLevel() ?: OuraResilienceLevel.UNKNOWN
        }.build()
    }

    private fun String.classifyResilienceLevel(): OuraResilienceLevel {
        return when (this) {
            "limited" -> OuraResilienceLevel.LIMITED
            "adequate" -> OuraResilienceLevel.ADEQUATE
            "solid" -> OuraResilienceLevel.SOLID
            "strong" -> OuraResilienceLevel.STRONG
            "exceptional" -> OuraResilienceLevel.EXCEPTIONAL
            else -> OuraResilienceLevel.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyResilienceConverter::class.java)
    }
}
