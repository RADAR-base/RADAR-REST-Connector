package org.radarbase.oura.converter

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.oura.user.User
import org.radarcns.connector.oura.OuraRingColor
import org.radarcns.connector.oura.OuraRingConfiguration
import org.radarcns.connector.oura.OuraRingDesign
import org.radarcns.connector.oura.OuraRingHardwareType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime

class OuraRingConfigurationConverter(
    private val topic: String = "connect_oura_ring_configuration",
) : OuraDataConverter {
    override fun processRecords(
        root: JsonNode,
        user: User,
    ): Sequence<Result<TopicData>> {
        val array =
            root.get("data")
                ?: return emptySequence()
        return array.asSequence()
            .mapCatching {
                val setUpAt = it["set_up_at"]
                val setupTimeInstant =
                    setUpAt?.textValue()?.let {
                        OffsetDateTime.parse(
                            it,
                        )
                    }?.toInstant()
                TopicData(
                    key = user.observationKey,
                    topic = topic,
                    offset = System.currentTimeMillis() / 1000,
                    value = it.toRingConfiguration(setupTimeInstant),
                )
            }
    }

    private fun JsonNode.toRingConfiguration(
        setupTime: Instant?,
    ): OuraRingConfiguration {
        val data = this
        return OuraRingConfiguration.newBuilder().apply {
            time = System.currentTimeMillis() / 1000.0
            timeReceived = System.currentTimeMillis() / 1000.0
            id = data.get("id")?.textValue()
            color = data.get("color")?.textValue()?.classifyColor() ?: OuraRingColor.UNKNOWN
            design = data.get("design")?.textValue()?.classifyDesign() ?: OuraRingDesign.UNKNOWN
            firmwareVersion = data.get("firmware_version")?.textValue()
            hardwareType = data.get("hardware_type")?.textValue()?.classifyHardware()
                ?: OuraRingHardwareType.UNKNOWN
            setUpAt = setupTime?.toEpochMilli()?.let { it / 1000.0 }
            size = data.get("size")?.intValue()
        }.build()
    }

    private fun String.classifyColor(): OuraRingColor {
        return when (this) {
            "glossy_black" -> OuraRingColor.GLOSSY_BLACK
            "stealth_black" -> OuraRingColor.STEALTH_BLACK
            "rose" -> OuraRingColor.ROSE
            "silver" -> OuraRingColor.SILVER
            "glossy_gold" -> OuraRingColor.GLOSSY_GOLD
            else -> OuraRingColor.UNKNOWN
        }
    }

    private fun String.classifyDesign(): OuraRingDesign {
        return when (this) {
            "heritage" -> OuraRingDesign.HERITAGE
            "horizon" -> OuraRingDesign.HORIZON
            else -> OuraRingDesign.UNKNOWN
        }
    }

    private fun String.classifyHardware(): OuraRingHardwareType {
        return when (this) {
            "gen1" -> OuraRingHardwareType.GEN1
            "gen2" -> OuraRingHardwareType.GEN2
            "gen2m" -> OuraRingHardwareType.GEN2M
            "gen3" -> OuraRingHardwareType.GEN3
            else -> OuraRingHardwareType.UNKNOWN
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OuraRingConfigurationConverter::class.java)
    }
}
