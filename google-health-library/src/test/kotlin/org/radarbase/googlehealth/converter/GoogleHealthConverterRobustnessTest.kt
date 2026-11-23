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
import com.fasterxml.jackson.databind.ObjectMapper
import org.radarbase.googlehealth.user.GoogleHealthUser
import org.radarcns.push.googlehealth.GoogleHealthDailyRestingHeartRate
import org.radarcns.push.googlehealth.GoogleHealthElectrocardiogram
import org.radarcns.push.googlehealth.GoogleHealthExercise
import org.radarcns.push.googlehealth.GoogleHealthFloors
import org.radarcns.push.googlehealth.GoogleHealthHeartRate
import org.radarcns.push.googlehealth.GoogleHealthSteps
import org.radarcns.push.googlehealth.GoogleHealthTotalCalories
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Made-up data in the shapes the Google Health API returns. */
class GoogleHealthConverterRobustnessTest {
    private val mapper = ObjectMapper()
    private val irnName = "users/me/dataTypes/irregular-rhythm-notification/dataPoints/9"
    private val user = GoogleHealthUser(
        id = "1",
        createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        projectId = "test-project",
        userId = "test-user",
        humanReadableUserId = null,
        sourceId = "test-source",
        externalId = null,
        isAuthorized = true,
        startDate = Instant.parse("2026-10-01T00:00:00Z"),
        endDate = null,
        serviceUserId = "1234567890",
    )

    private fun page(vararg points: String): JsonNode =
        mapper.readTree("""{"dataPoints": [${points.joinToString(",")}]}""")

    private fun exercise(name: String?): String {
        val nameField = name?.let { "\"name\": \"$EXERCISE_PREFIX$it\"," } ?: ""
        return """
            {$nameField "exercise": {
              "interval": {"startTime": "2026-10-20T10:00:00Z", "endTime": "2026-10-20T10:30:00Z",
                "startUtcOffset": "3600s"},
              "exerciseType": "WALKING", "activeDuration": "1500s",
              "metricsSummary": {"caloriesKcal": 100, "steps": "2500"}}}
        """
    }

    private fun ecg(name: String?): String {
        val nameField = name?.let { "\"name\": \"$ECG_PREFIX$it\"," } ?: ""
        return """
            {$nameField "electrocardiogram": {
              "interval": {"startTime": "2026-10-20T10:00:00Z"},
              "waveformSamples": [1, 2, 3], "samplingFrequencyHertz": 250}}
        """
    }

    private fun steps(count: String) = """
        {"steps": {"interval": {"startTime": "2026-10-20T10:00:00Z",
          "endTime": "2026-10-20T10:01:00Z"}, "count": $count}}
    """

    @Test
    fun exercisePointWithIdAboveLongMaxIsSkippedNotThePage() {
        val tree = page(exercise("1111"), exercise("18000000000000000000"), exercise("3333"))
        val ids = GoogleHealthExerciseAvroConverter("t").convert(tree, user)
            .map { (it.second as GoogleHealthExercise).id }
        assertEquals(listOf(1111L, 3333L), ids)
    }

    @Test
    fun exercisePointWithoutNameIsSkippedNotThePage() {
        val tree = page(exercise(null), exercise("2222"))
        assertEquals(1, GoogleHealthExerciseAvroConverter("t").convert(tree, user).size)
    }

    @Test
    fun ecgReadingWithoutNameIsSkippedNotThePage() {
        val tree = page(ecg(null), ecg("2026-10-20T10:00:00.000"))
        val records = GoogleHealthElectrocardiogramAvroConverter("t").convert(tree, user)
        assertEquals(3, records.size)
        assertEquals(
            "2026-10-20T10:00:00.000",
            (records.first().second as GoogleHealthElectrocardiogram).id,
        )
    }

    @Test
    fun irregularRhythmPointWithoutNameIsSkippedNotThePage() {
        val irn = { name: String? ->
            val nameField = name?.let { "\"name\": \"$it\"," } ?: ""
            """
                {$nameField "irregularRhythmNotification": {"alertWindows": [
                  {"startTime": "2026-10-20T10:00:00Z", "endTime": "2026-10-20T10:01:00Z",
                   "heartBeats": [
                     {"physicalTime": "2026-10-20T10:00:01Z", "beatsPerMinute": "80"}]}]}}
            """
        }
        val tree = page(irn(null), irn(irnName))
        val converter = GoogleHealthIrregularRhythmNotificationAvroConverter("t")
        assertEquals(1, converter.convert(tree, user).size)
    }

    @Test
    fun nonNumericStepCountIsDroppedNotZero() {
        val tree = page(steps("\"24\""), steps("24"), steps("\"abc\""), steps("null"))
        val counts = GoogleHealthStepsAvroConverter("t").convert(tree, user)
            .map { (it.second as GoogleHealthSteps).steps }
        assertEquals(listOf(24, 24), counts)
    }

    @Test
    fun nonNumericFloorCountIsDroppedNotZero() {
        val floors = { count: String ->
            """
                {"floors": {"interval": {"startTime": "2026-10-20T10:00:00Z",
                  "endTime": "2026-10-20T10:01:00Z"}, "count": $count}}
            """
        }
        val tree = page(floors("\"1\""), floors("\"abc\""))
        val counts = GoogleHealthFloorsAvroConverter("t").convert(tree, user)
            .map { (it.second as GoogleHealthFloors).floors }
        assertEquals(listOf(1), counts)
    }

    @Test
    fun kcalSumAsStringIsReadNotZero() {
        val bucket = { kcal: String ->
            """
                {"startTime": "2026-10-20T10:00:00Z", "endTime": "2026-10-20T10:01:00Z",
                 "totalCalories": {"kcalSum": $kcal}}
            """
        }
        val buckets = listOf(bucket("1.5"), bucket("\"1.25\""), bucket("\"abc\""))
        val tree = mapper.readTree("""{"rollupDataPoints": [${buckets.joinToString(",")}]}""")
        val calories = GoogleHealthTotalCaloriesAvroConverter("t").convert(tree, user)
            .map { (it.second as GoogleHealthTotalCalories).calories }
        assertEquals(listOf(1.5, 1.25), calories)
    }

    @Test
    fun respiratorySummaryWithoutStatsIsDropped() {
        val summary = { stats: String ->
            """
                {"respiratoryRateSleepSummary": {"sampleTime":
                  {"physicalTime": "2026-10-20T06:00:00Z"} $stats}}
            """
        }
        val tree = page(summary(""), summary(""", "fullSleepStats": {"breathsPerMinute": 14.2}"""))
        val converter = GoogleHealthRespiratoryRateSleepSummaryAvroConverter("t")
        assertEquals(1, converter.convert(tree, user).size)
    }

    @Test
    fun nullOrNonNumericHeartRateIsDroppedNotZero() {
        val heartRate = { bpm: String ->
            """
                {"heartRate": {"sampleTime": {"physicalTime": "2026-10-20T10:00:00Z"},
                  "beatsPerMinute": $bpm}}
            """
        }
        val tree = page(heartRate("\"59\""), heartRate("60"), heartRate("null"), heartRate("\"x\""))
        val values = GoogleHealthHeartRateAvroConverter("t").convert(tree, user)
            .map { (it.second as GoogleHealthHeartRate).heartRate }
        assertEquals(listOf(59, 60), values)
    }

    @Test
    fun nullOrNonNumericRestingHeartRateIsDroppedNotZero() {
        val resting = { bpm: String ->
            """
                {"dailyRestingHeartRate": {"date": {"year": 2026, "month": 10, "day": 20},
                  "beatsPerMinute": $bpm}}
            """
        }
        val tree = page(resting("\"55\""), resting("null"), resting("\"x\""))
        val values = GoogleHealthDailyRestingHeartRateAvroConverter("t").convert(tree, user)
            .map { (it.second as GoogleHealthDailyRestingHeartRate).restingHeartRate }
        assertEquals(listOf(55), values)
    }

    @Test
    fun nullOrNonNumericEcgSampleIsSkippedAndOthersKeepTheirTime() {
        val tree = page(
            """
                {"name": "${ECG_PREFIX}r1", "electrocardiogram": {
                  "interval": {"startTime": "2026-10-20T10:00:00Z"},
                  "waveformSamples": [10, null, "x", 40], "samplingFrequencyHertz": 250,
                  "millivoltsScalingFactor": "x", "leadNumber": null}}
            """,
        )
        val records = GoogleHealthElectrocardiogramAvroConverter("t").convert(tree, user)
            .map { it.second as GoogleHealthElectrocardiogram }
        assertEquals(listOf(10, 40), records.map { it.sample })
        val start = Instant.parse("2026-10-20T10:00:00Z").epochSecond.toDouble()
        assertEquals(listOf(start, start + 3.0 / 250), records.map { it.time })
        assertNull(records.first().millivoltsScalingFactor)
        assertNull(records.first().leadNumber)
    }

    companion object {
        private const val EXERCISE_PREFIX = "users/1234567890/dataTypes/exercise/dataPoints/"
        private const val ECG_PREFIX = "users/me/dataTypes/electrocardiogram/dataPoints/"
    }
}
