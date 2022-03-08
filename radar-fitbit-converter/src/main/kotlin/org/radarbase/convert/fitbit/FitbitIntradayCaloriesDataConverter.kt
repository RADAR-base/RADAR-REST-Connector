package org.radarbase.convert.fitbit

import com.fasterxml.jackson.databind.JsonNode
import org.radarcns.connector.fitbit.FitbitIntradayCalories
import java.time.LocalTime
import java.time.ZonedDateTime

class FitbitIntradayCaloriesDataConverter(
    private val caloriesTopic: String,
) : FitbitDataConverter {

    override fun processRecords(
        dateRange: DateRange, root: JsonNode, timeReceived: Double
    ): Sequence<Result<TopicData>> {
        val intraday = root.optObject("activities-calories-intraday")
            ?: return emptySequence()
        val dataset = intraday.optArray("dataset")
            ?: return emptySequence()
        val interval: Int = intraday.getRecordInterval(60)

        // Used as the date to convert the local times in the dataset to absolute times.
        val startDate: ZonedDateTime = dateRange.end
        return dataset.asSequence()
            .mapCatching { activity ->
                val localTime = LocalTime.parse(activity.get("time").asText())
                val time = startDate.with(localTime).toInstant()
                TopicData(
                    sourceOffset = time,
                    topic = caloriesTopic,
                    value = FitbitIntradayCalories(
                        time.toEpochMilli() / 1000.0,
                        timeReceived,
                        interval,
                        activity.get("value").asDouble(),
                        activity.get("level").asInt(),
                        activity.get("mets").asDouble(),
                    )
                )
            }
    }
}
