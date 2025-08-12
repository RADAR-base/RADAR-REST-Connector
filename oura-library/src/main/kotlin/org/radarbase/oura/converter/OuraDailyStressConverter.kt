package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraDailyStress
import org.radarcns.connector.oura.OuraDaySummaryType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class OuraDailyStressConverter(
    private val topic: String = "connect_oura_daily_stress",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array = root.get("data")
            ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val localDate = LocalDate.parse(
                    it["day"].textValue(),
                    DateTimeFormatter.ISO_LOCAL_DATE,
                )
                val startInstant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = startInstant.toEpoch(),
                    value = it.toDailyStress(startInstant),
                )
            }
    }

    private fun JsonNode.toDailyStress(
        startTime: Instant,
    ): OuraDailyStress {
        val data = this
        return OuraDailyStress.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            day = data.get("day")?.textValue()
            stressHigh = data.get("stress_high")?.intValue()
            recoveryHigh = data.get("recovery_high")?.intValue()
            daySummary = data.get("day_summary")?.textValue()?.classifyDaySummaryType()
        }.build()
    }

    private fun String.classifyDaySummaryType(): OuraDaySummaryType {
        return when (this.lowercase()) {
            "normal" -> OuraDaySummaryType.NORMAL
            "stressful" -> OuraDaySummaryType.STRESSFUL
            "restored" -> OuraDaySummaryType.RESTORED
            else -> OuraDaySummaryType.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyStressConverter::class.java)
    }
}
