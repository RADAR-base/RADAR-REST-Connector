package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraSleepPhase
import org.radarcns.connector.oura.OuraSleepPhaseType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime

class OuraSleepPhaseConverter(
    private val topic: String = "connect_oura_sleep_phase",
) : OuraDataConverter {

    final val SLEEP_PHASE_INTERVAL = 300 // in seconds

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
        val items = this.get("sleep_phase_5_min")?.textValue()?.toCharArray()
        return if (items == null) {
            emptySequence()
        } else {
            items.asSequence()
                .mapIndexedCatching { index, value ->
                    val offset = SLEEP_PHASE_INTERVAL * index
                    val time = startTimeEpoch + offset
                    TopicData(
                        key = user.observationKey,
                        topic = topic,
                        offset = time.toLong(),
                        value = toSleepPhase(
                            time,
                            timeReceivedEpoch,
                            id,
                            value.toString(),
                        ),
                    )
                }
        }
    }

    private fun toSleepPhase(
        startTimeEpoch: Double,
        timeReceivedEpoch: Double,
        idString: String,
        value: String,
    ): OuraSleepPhase {
        return OuraSleepPhase.newBuilder().apply {
            id = idString
            time = startTimeEpoch
            timeReceived = timeReceivedEpoch
            phase = value.classify()
        }.build()
    }

    private fun String.classify(): OuraSleepPhaseType {
        return when (this) {
            "1" -> OuraSleepPhaseType.DEEP
            "2" -> OuraSleepPhaseType.LIGHT
            "3" -> OuraSleepPhaseType.REM
            "4" -> OuraSleepPhaseType.AWAKE
            else -> OuraSleepPhaseType.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraSleepPhaseConverter::class.java)
    }
}
