package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraDailyCardiovascularAge
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class OuraDailyCardiovascularAgeConverter(
    private val topic: String = "connect_oura_daily_cardiovascular_age",
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
                    value = it.toDailyCardiovascularAge(startInstant),
                )
            }
    }

    private fun JsonNode.toDailyCardiovascularAge(
        startTime: Instant,
    ): OuraDailyCardiovascularAge {
        val data = this
        return OuraDailyCardiovascularAge.newBuilder().apply {
            time = startTime.toEpochMilli() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            day = data.get("day")?.textValue()
            vascularAge = data.get("vascular_age")?.intValue()
        }.build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraDailyCardiovascularAgeConverter::class.java)
    }
}
