package org.radarbase.googlehealth.util

import org.radarcns.connector.fitbit.FitbitActivityHeartRate
import org.radarcns.connector.fitbit.FitbitActivityLogRecord
import org.radarcns.connector.fitbit.FitbitBreathingRate
import org.radarcns.connector.fitbit.FitbitIntradayCalories
import org.radarcns.connector.fitbit.FitbitIntradayHeartRate
import org.radarcns.connector.fitbit.FitbitIntradayHeartRateVariability
import org.radarcns.connector.fitbit.FitbitIntradaySpo2
import org.radarcns.connector.fitbit.FitbitIntradaySteps
import org.radarcns.connector.fitbit.FitbitRestingHeartRate
import org.radarcns.connector.fitbit.FitbitSkinTemperature
import org.radarcns.connector.fitbit.FitbitSleepClassic
import org.radarcns.connector.fitbit.FitbitSleepStage

inline fun intradaySteps(
    block: FitbitIntradaySteps.Builder.() -> Unit,
): FitbitIntradaySteps {
    return FitbitIntradaySteps.newBuilder().apply(block).build()
}

inline fun intradayHeartRate(
    block: FitbitIntradayHeartRate.Builder.() -> Unit,
): FitbitIntradayHeartRate {
    return FitbitIntradayHeartRate.newBuilder().apply(block).build()
}

inline fun intradayHeartRateVariability(
    block: FitbitIntradayHeartRateVariability.Builder.() -> Unit,
): FitbitIntradayHeartRateVariability {
    return FitbitIntradayHeartRateVariability.newBuilder().apply(block).build()
}

inline fun intradaySpo2(
    block: FitbitIntradaySpo2.Builder.() -> Unit,
): FitbitIntradaySpo2 {
    return FitbitIntradaySpo2.newBuilder().apply(block).build()
}

inline fun restingHeartRate(
    block: FitbitRestingHeartRate.Builder.() -> Unit,
): FitbitRestingHeartRate {
    return FitbitRestingHeartRate.newBuilder().apply(block).build()
}

inline fun breathingRate(
    block: FitbitBreathingRate.Builder.() -> Unit,
): FitbitBreathingRate {
    return FitbitBreathingRate.newBuilder().apply(block).build()
}

inline fun skinTemperature(
    block: FitbitSkinTemperature.Builder.() -> Unit,
): FitbitSkinTemperature {
    return FitbitSkinTemperature.newBuilder().apply(block).build()
}

inline fun sleepClassic(
    block: FitbitSleepClassic.Builder.() -> Unit,
): FitbitSleepClassic {
    return FitbitSleepClassic.newBuilder().apply(block).build()
}

inline fun sleepStage(
    block: FitbitSleepStage.Builder.() -> Unit,
): FitbitSleepStage {
    return FitbitSleepStage.newBuilder().apply(block).build()
}

inline fun activityLogRecord(
    block: FitbitActivityLogRecord.Builder.() -> Unit,
): FitbitActivityLogRecord {
    return FitbitActivityLogRecord.newBuilder().apply(block).build()
}

inline fun activityHeartRate(
    block: FitbitActivityHeartRate.Builder.() -> Unit,
): FitbitActivityHeartRate {
    return FitbitActivityHeartRate.newBuilder().apply(block).build()
}

inline fun intradayCalories(
    block: FitbitIntradayCalories.Builder.() -> Unit,
): FitbitIntradayCalories {
    return FitbitIntradayCalories.newBuilder().apply(block).build()
}
