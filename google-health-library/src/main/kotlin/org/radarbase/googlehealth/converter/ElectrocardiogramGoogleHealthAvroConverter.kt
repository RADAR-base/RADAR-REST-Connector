/*
 * Copyright 2026 King's College London
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.googlehealth.converter

import com.fasterxml.jackson.databind.JsonNode
import org.apache.avro.specific.SpecificRecord
import org.radarbase.googlehealth.user.User
import org.radarbase.googlehealth.util.googleHealthElectrocardiogram
import java.time.Instant

/**
 * Emits one record per ECG waveform sample. Sample i is timed at the reading start plus
 * i / samplingFrequencyHertz seconds and carries the raw waveform value as reported by the
 * device (an ADC count; divide by millivoltsScalingFactor for millivolts). The reading-level
 * metadata (heart rate, sampling parameters, device info) is repeated on every sample's record,
 * linked by the shared reading id.
 */
class ElectrocardiogramGoogleHealthAvroConverter(topic: String) : GoogleHealthAvroConverter(topic) {
    override fun convertDataPoint(
        point: JsonNode,
        user: User,
    ): List<Pair<SpecificRecord, SpecificRecord>> {
        val data = point["electrocardiogram"] ?: return emptyList()
        val start = data["interval"]?.get("startTime")?.asText()
            ?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return emptyList()
        val id = (point["name"] ?: point["dataPointName"])?.asText()?.substringAfterLast('/')
            ?: run {
                logger.warn("Dropping electrocardiogram data point with no usable id for user={}", user.versionedId)
                return emptyList()
            }
        val samples = data["waveformSamples"]?.takeIf { it.isArray } ?: return emptyList()
        val frequency = data["samplingFrequencyHertz"]?.takeIf { !it.isNull }?.asInt()?.takeIf { it > 0 }
            ?: return emptyList()

        val device = data["medicalDeviceInfo"]
        val beatsPerMinuteAvg = data["beatsPerMinuteAvg"]?.takeIf { !it.isNull }?.asText()?.toIntOrNull()
        val scalingFactor = data["millivoltsScalingFactor"]?.takeIf { !it.isNull }?.asInt()
        val leadNumber = data["leadNumber"]?.takeIf { !it.isNull }?.asInt()
        val deviceModel = device?.get("deviceModel")?.asText()
        val firmwareVersion = device?.get("firmwareVersion")?.asText()
        val featureVersion = device?.get("featureVersion")?.asText()

        val startSec = epochSeconds(start)
        val received = nowEpochSeconds()
        return samples.mapIndexed { i, sampleNode ->
            val record = googleHealthElectrocardiogram {
                time = startSec + i.toDouble() / frequency
                timeReceived = received
                this.id = id
                sample = sampleNode.asInt()
                this.beatsPerMinuteAvg = beatsPerMinuteAvg
                this.samplingFrequencyHertz = frequency
                this.millivoltsScalingFactor = scalingFactor
                this.leadNumber = leadNumber
                this.deviceModel = deviceModel
                this.firmwareVersion = firmwareVersion
                this.featureVersion = featureVersion
            }
            user.observationKey to record
        }
    }
}
