/*
 * Copyright 2018 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.radarbase.connect.rest.fitbit.converter

import io.confluent.connect.avro.AvroData
import okhttp3.Headers
import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.rest.RestSourceConnectorConfig
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest
import org.radarbase.connect.rest.request.RestRequest
import org.radarbase.convert.fitbit.FitbitDataConverter
import org.radarbase.convert.fitbit.TopicData
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Instant
import java.util.*

/**
 * Abstract class to help convert Fitbit data to Avro Data.
 */
class FitbitAvroConverter(
    private val avroData: AvroData,
    private val dataConverterCreator: (FitbitRestSourceConnectorConfig) -> FitbitDataConverter
) : PayloadToSourceRecordConverter {
    private lateinit var converter: FitbitDataConverter
    override fun initialize(config: RestSourceConnectorConfig) {
        converter = dataConverterCreator(config as FitbitRestSourceConnectorConfig)
    }
    @Throws(IOException::class)
    override fun convert(
        restRequest: RestRequest, headers: Headers, data: ByteArray?
    ): Collection<SourceRecord> {
        data ?: throw IOException("Failed to read body")
        val activities = FitbitRequestGenerator.JSON_READER.readTree(data)
        val user = (restRequest as FitbitRestRequest).user
        val key = user.getObservationKey(avroData)
        val timeReceived = System.currentTimeMillis() / 1000.0
        return converter.processRecords(
            restRequest.dateRange,
            activities,
            timeReceived)
            .mapNotNull { r -> r.fold(
                {
                    it
                },
                {
                    logger.error("Data conversion failed for {} of user {}",
                        restRequest.request.url, restRequest.user.userId, it)
                    null
                })
            }
            .filter { t -> validateRecord(restRequest, t) }
            .map { t ->
                val avro = avroData.toConnectData(t.value.schema, t.value)
                val offset: Map<String, *> = Collections.singletonMap(
                    PayloadToSourceRecordConverter.TIMESTAMP_OFFSET_KEY,
                    t.sourceOffset.toEpochMilli())
                SourceRecord(restRequest.getPartition(), offset, t.topic,
                    key.schema(), key.value(), avro.schema(), avro.value())
            }
            .toList()
    }

    private fun validateRecord(request: FitbitRestRequest, record: TopicData): Boolean {
        val endDate = request.user.endDate ?: return true
        val timeField = record.value.schema.getField("time")
        return if (timeField != null) {
            val time = (record.value[timeField.pos()] as Double * 1000.0).toLong()
            Instant.ofEpochMilli(time) < endDate
        } else true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            FitbitAvroConverter::class.java)
    }
}
