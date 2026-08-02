package org.radarbase.googlehealth.util

import org.radarcns.push.googlehealth.GoogleHealthExerciseHeartRate
import org.radarcns.push.googlehealth.GoogleHealthExercise
import org.radarcns.push.googlehealth.GoogleHealthSource
import org.radarcns.push.googlehealth.GoogleHealthRespiratoryRateSleepSummary
import org.radarcns.push.googlehealth.GoogleHealthTotalCalories
import org.radarcns.push.googlehealth.GoogleHealthHeartRate
import org.radarcns.push.googlehealth.GoogleHealthHeartRateVariability
import org.radarcns.push.googlehealth.GoogleHealthOxygenSaturation
import org.radarcns.push.googlehealth.GoogleHealthSteps
import org.radarcns.push.googlehealth.GoogleHealthFloors
import org.radarcns.push.googlehealth.GoogleHealthSedentaryPeriod
import org.radarcns.push.googlehealth.GoogleHealthActivityLevel
import org.radarcns.push.googlehealth.GoogleHealthDailyRestingHeartRate
import org.radarcns.push.googlehealth.GoogleHealthDailySleepTemperatureDerivations
import org.radarcns.push.googlehealth.GoogleHealthSleepClassic
import org.radarcns.push.googlehealth.GoogleHealthSleepStage
import org.radarcns.push.googlehealth.GoogleHealthElectrocardiogram
import org.radarcns.push.googlehealth.GoogleHealthIrregularRhythmNotification

inline fun googleHealthSource(block: GoogleHealthSource.Builder.() -> Unit): GoogleHealthSource =
    GoogleHealthSource.newBuilder().apply(block).build()

inline fun googleHealthElectrocardiogram(block: GoogleHealthElectrocardiogram.Builder.() -> Unit): GoogleHealthElectrocardiogram =
    GoogleHealthElectrocardiogram.newBuilder().apply(block).build()

inline fun googleHealthIrregularRhythmNotification(block: GoogleHealthIrregularRhythmNotification.Builder.() -> Unit): GoogleHealthIrregularRhythmNotification =
    GoogleHealthIrregularRhythmNotification.newBuilder().apply(block).build()

inline fun googleHealthSteps(block: GoogleHealthSteps.Builder.() -> Unit): GoogleHealthSteps =
    GoogleHealthSteps.newBuilder().apply(block).build()

inline fun googleHealthFloors(block: GoogleHealthFloors.Builder.() -> Unit): GoogleHealthFloors =
    GoogleHealthFloors.newBuilder().apply(block).build()

inline fun googleHealthSedentaryPeriod(block: GoogleHealthSedentaryPeriod.Builder.() -> Unit): GoogleHealthSedentaryPeriod =
    GoogleHealthSedentaryPeriod.newBuilder().apply(block).build()

inline fun googleHealthActivityLevel(block: GoogleHealthActivityLevel.Builder.() -> Unit): GoogleHealthActivityLevel =
    GoogleHealthActivityLevel.newBuilder().apply(block).build()

inline fun googleHealthHeartRate(block: GoogleHealthHeartRate.Builder.() -> Unit): GoogleHealthHeartRate =
    GoogleHealthHeartRate.newBuilder().apply(block).build()

inline fun googleHealthHeartRateVariability(block: GoogleHealthHeartRateVariability.Builder.() -> Unit): GoogleHealthHeartRateVariability =
    GoogleHealthHeartRateVariability.newBuilder().apply(block).build()

inline fun googleHealthOxygenSaturation(block: GoogleHealthOxygenSaturation.Builder.() -> Unit): GoogleHealthOxygenSaturation =
    GoogleHealthOxygenSaturation.newBuilder().apply(block).build()

inline fun googleHealthDailyRestingHeartRate(block: GoogleHealthDailyRestingHeartRate.Builder.() -> Unit): GoogleHealthDailyRestingHeartRate =
    GoogleHealthDailyRestingHeartRate.newBuilder().apply(block).build()

inline fun googleHealthRespiratoryRateSleepSummary(block: GoogleHealthRespiratoryRateSleepSummary.Builder.() -> Unit): GoogleHealthRespiratoryRateSleepSummary =
    GoogleHealthRespiratoryRateSleepSummary.newBuilder().apply(block).build()

inline fun googleHealthDailySleepTemperatureDerivations(block: GoogleHealthDailySleepTemperatureDerivations.Builder.() -> Unit): GoogleHealthDailySleepTemperatureDerivations =
    GoogleHealthDailySleepTemperatureDerivations.newBuilder().apply(block).build()

inline fun googleHealthSleepClassic(block: GoogleHealthSleepClassic.Builder.() -> Unit): GoogleHealthSleepClassic =
    GoogleHealthSleepClassic.newBuilder().apply(block).build()

inline fun googleHealthSleepStage(block: GoogleHealthSleepStage.Builder.() -> Unit): GoogleHealthSleepStage =
    GoogleHealthSleepStage.newBuilder().apply(block).build()

inline fun googleHealthExercise(block: GoogleHealthExercise.Builder.() -> Unit): GoogleHealthExercise =
    GoogleHealthExercise.newBuilder().apply(block).build()

inline fun googleHealthExerciseHeartRate(block: GoogleHealthExerciseHeartRate.Builder.() -> Unit): GoogleHealthExerciseHeartRate =
    GoogleHealthExerciseHeartRate.newBuilder().apply(block).build()

inline fun googleHealthTotalCalories(block: GoogleHealthTotalCalories.Builder.() -> Unit): GoogleHealthTotalCalories =
    GoogleHealthTotalCalories.newBuilder().apply(block).build()
