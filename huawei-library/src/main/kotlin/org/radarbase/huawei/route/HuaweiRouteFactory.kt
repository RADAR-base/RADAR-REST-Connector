/*
 * Copyright 2026 Onsentia
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

package org.radarbase.huawei.route

import org.apache.avro.specific.SpecificRecord
import org.radarbase.huawei.converter.FieldValues
import org.radarcns.connector.huawei.HuaweiActiveHours
import org.radarcns.connector.huawei.HuaweiCgmBloodGlucose
import org.radarcns.connector.huawei.HuaweiContinuousActivityStatistics
import org.radarcns.connector.huawei.HuaweiContinuousAltitudeStatistics
import org.radarcns.connector.huawei.HuaweiContinuousBloodGlucoseStatistics
import org.radarcns.connector.huawei.HuaweiContinuousBodyBloodPressureStatistics
import org.radarcns.connector.huawei.HuaweiContinuousBreatheRateStatistics
import org.radarcns.connector.huawei.HuaweiContinuousCaloriesBurnt
import org.radarcns.connector.huawei.HuaweiContinuousCaloriesBurntTotal
import org.radarcns.connector.huawei.HuaweiContinuousDistanceDelta
import org.radarcns.connector.huawei.HuaweiContinuousDistanceTotal
import org.radarcns.connector.huawei.HuaweiContinuousEcgDetail
import org.radarcns.connector.huawei.HuaweiContinuousExerciseIntensity
import org.radarcns.connector.huawei.HuaweiContinuousExerciseIntensityV2
import org.radarcns.connector.huawei.HuaweiContinuousExerciseIntensityV2Statistics
import org.radarcns.connector.huawei.HuaweiContinuousSleepFragment
import org.radarcns.connector.huawei.HuaweiContinuousSpo2Statistics
import org.radarcns.connector.huawei.HuaweiContinuousStepsDelta
import org.radarcns.connector.huawei.HuaweiContinuousStepsTotal
import org.radarcns.connector.huawei.HuaweiDailyActivitySummary
import org.radarcns.connector.huawei.HuaweiEmotion
import org.radarcns.connector.huawei.HuaweiHealthRecordDynamicBp
import org.radarcns.connector.huawei.HuaweiHealthRecordHeartRateAlert
import org.radarcns.connector.huawei.HuaweiHealthRecordHyperthermia
import org.radarcns.connector.huawei.HuaweiHealthRecordLowSpo2Alert
import org.radarcns.connector.huawei.HuaweiHealthRecordMenstrualCycle
import org.radarcns.connector.huawei.HuaweiHealthRecordSleep
import org.radarcns.connector.huawei.HuaweiHeartRateVariability
import org.radarcns.connector.huawei.HuaweiRestingCaloriesStatistics
import org.radarcns.connector.huawei.HuaweiSleepOnOffBedRecord
import org.radarcns.connector.huawei.HuaweiSleepRespiratoryDetail
import org.radarcns.connector.huawei.HuaweiSleepRespiratoryEvent
import org.radarcns.connector.huawei.HuaweiStatistics
import org.radarcns.connector.huawei.HuaweiVo2Max
import java.time.Instant

/**
 * Registry of every Huawei Health Kit data type this connector supports, mapping each one to the
 * Kafka Connect REST endpoint (`sampleSet:polymerize`, `healthRecords`, or `activityRecords`) and
 * Avro record type documented in the `radar-huawei-connector` schema specification
 * (RADAR-base/RADAR-Schemas, `huawei_schemas` branch).
 *
 * Huawei `dataTypeName`/`subDataTypeName` values below are taken verbatim from that
 * specification's `doc` strings (prefixed with the vendor namespace `com.huawei.`), which in turn
 * describe the Huawei Health Kit REST Data API's own data type identifiers.
 *
 * Field-value key names used in the record builders are Huawei Health Kit `Field` identifiers
 * (snake_case, matching the on-device HiHealth SDK's public `Field.FIELD_*` constant family, e.g.
 * `steps_delta`, `calories`, `avg`/`max`/`min`/`last`). Where a field is not among Huawei's widely
 * documented constants, the snake_case form of the Avro field's own name is used as a best-effort
 * default (see [snake]) — verify against a live API response and adjust the key strings in this
 * file if Huawei's actual response uses different names.
 *
 * @author yatharthranjan
 */
object HuaweiRouteFactory {

    private const val VENDOR_PREFIX = "com.huawei."

    private fun Instant.toEpoch(): Double = toEpochMilli() / 1000.0

    /** Best-effort camelCase -> snake_case conversion for deriving a Huawei field key from an Avro field name. */
    private fun snake(name: String): String =
        SNAKE_CASE_BOUNDARY.replace(
            name,
        ) { "${it.groupValues[1]}_${it.groupValues[2]}" }.lowercase()

    private val SNAKE_CASE_BOUNDARY = Regex("([a-z0-9])([A-Z])")

    private fun HuaweiStatistics.Builder.populateCommon(
        startTime: Instant,
        endTime: Instant?,
        timeReceived: Instant,
        fields: FieldValues,
    ) {
        time = startTime.toEpoch()
        this.timeReceived = timeReceived.toEpoch()
        this.endTime = endTime?.toEpoch()
        avg = fields.getDouble("avg")
        max = fields.getDouble("max")
        min = fields.getDouble("min")
        last = fields.getDouble("last")
        count = fields.getInt("count")
    }

    /** Data types that reuse the generic [HuaweiStatistics] schema: (config key, Huawei data type name, default topic). */
    private val genericStatisticsTypes = listOf(
        Triple(
            "continuous_body_fat_rate_statistics",
            "continuous.body.fat.rate.statistics",
            "connect_huawei_continuous_body_fat_rate_statistics",
        ),
        Triple(
            "continuous_body_temperature_rest_statistics",
            "continuous.body.temperature.rest.statistics",
            "connect_huawei_continuous_body_temperature_rest_statistics",
        ),
        Triple(
            "continuous_body_temperature_statistics",
            "continuous.body.temperature.statistics",
            "connect_huawei_continuous_body_temperature_statistics",
        ),
        Triple(
            "continuous_calories_bmr_statistics",
            "continuous.calories.bmr.statistics",
            "connect_huawei_continuous_calories_bmr_statistics",
        ),
        Triple(
            "continuous_exercise_heart_rate_statistics",
            "continuous.exercise_heart_rate.statistics",
            "connect_huawei_continuous_exercise_heart_rate_statistics",
        ),
        Triple(
            "continuous_heart_rate_statistics",
            "continuous.heart_rate.statistics",
            "connect_huawei_continuous_heart_rate_statistics",
        ),
        Triple(
            "continuous_power_statistics",
            "continuous.power.statistics",
            "connect_huawei_continuous_power_statistics",
        ),
        Triple(
            "continuous_skin_temperature_statistics",
            "continuous.skin.temperature.statistics",
            "connect_huawei_continuous_skin_temperature_statistics",
        ),
        Triple(
            "continuous_speed_statistics",
            "continuous.speed.statistics",
            "connect_huawei_continuous_speed_statistics",
        ),
        Triple(
            "continuous_steps_rate_statistics",
            "continuous.steps.rate.statistics",
            "connect_huawei_continuous_steps_rate_statistics",
        ),
        Triple(
            "continuous_stroke_rate_statistics",
            "continuous.stroke_rate.statistics",
            "connect_huawei_continuous_stroke_rate_statistics",
        ),
        Triple(
            "instantaneous_resting_heart_rate_statistics",
            "instantaneous.resting_heart_rate.statistics",
            "connect_huawei_instantaneous_resting_heart_rate_statistics",
        ),
        Triple(
            "instantaneous_stress_statistics",
            "instantaneous.stress.statistics",
            "connect_huawei_instantaneous_stress_statistics",
        ),
        Triple("vo2max_statistics", "vo2max.statistics", "connect_huawei_vo2max_statistics"),
    )

    /** Full registry of Huawei Health Kit data types supported by this connector. */
    val definitions: List<HuaweiRouteDefinition> = buildList {
        add(
            HuaweiRouteDefinition(
                "activity_record",
                "connect_huawei_activity_record",
            ) { repo, topic ->
                HuaweiActivityRecordRoute(repo, topic)
            },
        )

        // cgm_blood_glucose (+ .statistics variant)
        add(
            sampleSetDefinition(
                "cgm_blood_glucose",
                "cgm_blood_glucose",
                "connect_huawei_cgm_blood_glucose",
            ) { f, start, _, received ->
                HuaweiCgmBloodGlucose.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    level = f.getDouble("level")
                    avg = f.getInt("avg")
                    max = f.getInt("max")
                    min = f.getInt("min")
                    last = f.getInt("last")
                }.build()
            },
        )
        add(
            sampleSetDefinition(
                "cgm_blood_glucose_statistics",
                "cgm_blood_glucose.statistics",
                "connect_huawei_cgm_blood_glucose_statistics",
            ) { f, start, _, received ->
                HuaweiCgmBloodGlucose.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    level = f.getDouble("level")
                    avg = f.getInt("avg")
                    max = f.getInt("max")
                    min = f.getInt("min")
                    last = f.getInt("last")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "daily_activity_summary",
                "daily_activity_summary",
                "connect_huawei_daily_activity_summary",
            ) { f, start, end, received ->
                HuaweiDailyActivitySummary.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    steps = f.getInt("steps")
                    activeCalories = f.getInt("calories")
                    exerciseTime = f.getInt("exercise_time")
                    activeHours = f.getInt("active_hours")
                    stepsGoal = f.getInt("steps_target")
                    activeCaloriesGoal = f.getInt("calories_target")
                    exerciseTimeGoal = f.getInt("exercise_time_target")
                    activeHoursGoal = f.getInt("active_hours_target")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "active_hours",
                "active_hours",
                "connect_huawei_active_hours",
            ) { f, start, end, received ->
                f.toActiveHours(start, end, received)
            },
        )
        add(
            sampleSetDefinition(
                "active_hours_statistics",
                "active_hours.statistics",
                "connect_huawei_active_hours_statistics",
            ) { f, start, end, received ->
                f.toActiveHours(start, end, received)
            },
        )

        add(
            sampleSetDefinition(
                "continuous_activity_fragment",
                "continuous.activity.fragment",
                "connect_huawei_continuous_activity_fragment",
            ) { f, start, end, received ->
                f.toContinuousActivityStatistics(start, end, received)
            },
        )
        add(
            sampleSetDefinition(
                "continuous_activity_statistics",
                "continuous.activity.statistics",
                "connect_huawei_continuous_activity_statistics",
            ) { f, start, end, received ->
                f.toContinuousActivityStatistics(start, end, received)
            },
        )

        add(
            sampleSetDefinition(
                "continuous_altitude_statistics",
                "continuous.altitude.statistics",
                "connect_huawei_continuous_altitude_statistics",
            ) { f, start, end, received ->
                HuaweiContinuousAltitudeStatistics.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    avg = f.getDouble("avg")
                    max = f.getDouble("max")
                    min = f.getDouble("min")
                    ascentTotal = f.getDouble("ascent_total")
                    descentTotal = f.getDouble("descent_total")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "continuous_blood_glucose_statistics",
                "continuous.blood_glucose.statistics",
                "connect_huawei_continuous_blood_glucose_statistics",
            ) { f, start, end, received ->
                HuaweiContinuousBloodGlucoseStatistics.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    avg = f.getDouble("avg")
                    max = f.getDouble("max")
                    min = f.getDouble("min")
                    correlationWithMealtime = f.getInt("correlate_mealtime")
                    meal = f.getInt("meal")
                    correlationWithSleepState = f.getInt("correlate_sleep")
                    sampleSource = f.getInt("sample_source")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "continuous_breathe_rate_statistics",
                "continuous.breathe_rate.statistics",
                "connect_huawei_continuous_breathe_rate_statistics",
            ) { f, start, end, received ->
                HuaweiContinuousBreatheRateStatistics.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    maxBreatheRate = f.getInt("max_breathe_rate")
                    minBreatheRate = f.getInt("min_breathe_rate")
                    avgBreatheRate = f.getInt("avg_breathe_rate")
                    minBreathrateBaseline = f.getInt("min_breathrate_baseline")
                    maxBreathrateBaseline = f.getInt("max_breathrate_baseline")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "continuous_body_blood_pressure_statistics",
                "continuous.body.blood_pressure.statistics",
                "connect_huawei_continuous_body_blood_pressure_statistics",
            ) { f, start, end, received ->
                HuaweiContinuousBodyBloodPressureStatistics.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    systolicPressureAvg = f.getDouble("systolic_pressure_avg")
                    systolicPressureMax = f.getDouble("systolic_pressure_max")
                    systolicPressureMin = f.getDouble("systolic_pressure_min")
                    diastolicPressureAvg = f.getDouble("diastolic_pressure_avg")
                    diastolicPressureMax = f.getDouble("diastolic_pressure_max")
                    diastolicPressureMin = f.getDouble("diastolic_pressure_min")
                    sphygmusAvg = f.getDouble("sphygmus_avg")
                    sphygmusMax = f.getDouble("sphygmus_max")
                    sphygmusMin = f.getDouble("sphygmus_min")
                    sphygmusLast = f.getDouble("sphygmus_last")
                }.build()
            },
        )

        genericStatisticsTypes.forEach { (key, dataType, topic) ->
            add(
                sampleSetDefinition(key, dataType, topic) { f, start, end, received ->
                    HuaweiStatistics.newBuilder().apply {
                        populateCommon(
                            start,
                            end,
                            received,
                            f,
                        )
                    }.build()
                },
            )
        }

        add(
            sampleSetDefinition(
                "continuous_calories_burnt",
                "continuous.calories.burnt",
                "connect_huawei_continuous_calories_burnt",
            ) { f, start, end, received ->
                HuaweiContinuousCaloriesBurnt.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    calories = f.getDouble("calories")
                }.build()
            },
        )
        add(
            sampleSetDefinition(
                "continuous_calories_consumed",
                "continuous.calories.consumed",
                "connect_huawei_continuous_calories_consumed",
            ) { f, start, end, received ->
                HuaweiContinuousCaloriesBurnt.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    calories = f.getDouble("calories")
                }.build()
            },
        )
        add(
            sampleSetDefinition(
                "continuous_calories_burnt_total",
                "continuous.calories.burnt.total",
                "connect_huawei_continuous_calories_burnt_total",
            ) { f, start, end, received ->
                HuaweiContinuousCaloriesBurntTotal.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    caloriesTotal = f.getDouble("calories_total")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "continuous_distance_delta",
                "continuous.distance.delta",
                "connect_huawei_continuous_distance_delta",
            ) { f, start, end, received ->
                HuaweiContinuousDistanceDelta.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    distanceDelta = f.getDouble("distance_delta")
                }.build()
            },
        )
        add(
            sampleSetDefinition(
                "continuous_distance_total",
                "continuous.distance.total",
                "connect_huawei_continuous_distance_total",
            ) { f, start, end, received ->
                HuaweiContinuousDistanceTotal.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    distance = f.getDouble("distance_total")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "continuous_ecg_detail",
                "continuous.ecg_detail",
                "connect_huawei_continuous_ecg_detail",
            ) { f, start, end, received ->
                HuaweiContinuousEcgDetail.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    ecgRecordId = f.getString("record_id")
                    averageHeartRate = f.getInt("avg_heart_rate")
                    ecgArrhythmiaType = f.getInt("arrhythmia_type")
                    ecgArrhythmiaResult = f.getInt("arrhythmia_result")
                    userSymptom = f.getString("user_symptom")
                    samplingFrequency = f.getInt("sampling_frequency")
                    voltageData = f.getString("voltage_data")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "continuous_exercise_intensity",
                "continuous.exercise_intensity",
                "connect_huawei_continuous_exercise_intensity",
            ) { f, start, end, received ->
                f.toContinuousExerciseIntensity(start, end, received)
            },
        )
        add(
            sampleSetDefinition(
                "continuous_exercise_intensity_statistics",
                "continuous.exercise_intensity.statistics",
                "connect_huawei_continuous_exercise_intensity_statistics",
            ) { f, start, end, received ->
                f.toContinuousExerciseIntensity(start, end, received)
            },
        )

        add(
            sampleSetDefinition(
                "continuous_exercise_intensity_v2",
                "continuous.exercise_intensity.v2",
                "connect_huawei_continuous_exercise_intensity_v2",
            ) { f, start, end, received ->
                HuaweiContinuousExerciseIntensityV2.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    exerciseType = f.getInt("exercise_type")
                }.build()
            },
        )
        add(
            sampleSetDefinition(
                "continuous_exercise_intensity_v2_statistics",
                "continuous.exercise_intensity.v2.statistics",
                "connect_huawei_continuous_exercise_intensity_v2_statistics",
            ) { f, start, end, received ->
                HuaweiContinuousExerciseIntensityV2Statistics.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    zone1Duration = f.getInt("zone1_duration")
                    zone2Duration = f.getInt("zone2_duration")
                    zone3Duration = f.getInt("zone3_duration")
                    zone4Duration = f.getInt("zone4_duration")
                    zone5Duration = f.getInt("zone5_duration")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "continuous_sleep_fragment",
                "continuous.sleep.fragment",
                "connect_huawei_continuous_sleep_fragment",
            ) { f, start, end, received ->
                HuaweiContinuousSleepFragment.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    sleepState = f.getInt("sleep_state")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "continuous_spo2_statistics",
                "continuous.spo2.statistics",
                "connect_huawei_continuous_spo2_statistics",
            ) { f, start, end, received ->
                HuaweiContinuousSpo2Statistics.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    saturationAvg = f.getDouble("avg")
                    saturationMax = f.getDouble("max")
                    saturationMin = f.getDouble("min")
                    saturationLast = f.getDouble("last")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "continuous_steps_delta",
                "continuous.steps.delta",
                "connect_huawei_continuous_steps_delta",
            ) { f, start, end, received ->
                HuaweiContinuousStepsDelta.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    stepsDelta = f.getInt("steps_delta")
                }.build()
            },
        )
        add(
            sampleSetDefinition(
                "continuous_steps_total",
                "continuous.steps.total",
                "connect_huawei_continuous_steps_total",
            ) { f, start, end, received ->
                HuaweiContinuousStepsTotal.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    steps = f.getInt("steps")
                    duration = f.getInt("duration")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "emotion",
                "emotion",
                "connect_huawei_emotion",
            ) { f, start, _, received ->
                HuaweiEmotion.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    emotionStatus = f.getInt("emotion")
                }.build()
            },
        )

        add(
            healthRecordDefinition(
                "health_record_dynamic_bp",
                "health.record.dynamic_bp",
                "connect_huawei_health_record_dynamic_bp",
            ) { f, start, end, received ->
                f.toHealthRecordDynamicBp(start, end, received)
            },
        )
        add(
            healthRecordDefinition(
                "health_record_bradycardia",
                "health.record.bradycardia",
                "connect_huawei_health_record_bradycardia",
            ) { f, start, end, received ->
                f.toHealthRecordHeartRateAlert(start, end, received)
            },
        )
        add(
            healthRecordDefinition(
                "health_record_tachycardia",
                "health.record.tachycardia",
                "connect_huawei_health_record_tachycardia",
            ) { f, start, end, received ->
                f.toHealthRecordHeartRateAlert(start, end, received)
            },
        )
        add(
            healthRecordDefinition(
                "health_record_hyperthermia",
                "health.record.hyperthermia",
                "connect_huawei_health_record_hyperthermia",
            ) { f, start, end, received ->
                HuaweiHealthRecordHyperthermia.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    highBodyTemperatureAlarm = f.getFloat("high_body_temperature_alarm")
                }.build()
            },
        )
        add(
            healthRecordDefinition(
                "health_record_low_spo2_alert",
                "health.record.lowSpo2Alert",
                "connect_huawei_health_record_low_spo2_alert",
            ) { f, start, end, received ->
                HuaweiHealthRecordLowSpo2Alert.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    threshold = f.getFloat("threshold")
                    maxSpO2 = f.getFloat("max_spo2")
                    minSpO2 = f.getFloat("min_spo2")
                }.build()
            },
        )
        add(
            healthRecordDefinition(
                "health_record_menstrual_cycle",
                "health.record.menstrual_cycle",
                "connect_huawei_health_record_menstrual_cycle",
            ) { f, start, end, received ->
                HuaweiHealthRecordMenstrualCycle.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    recordday = f.getInt("record_day")
                    status = f.getInt("status")
                    substatus = f.getInt("sub_status")
                    remarks = f.getString("remarks")
                    timezone = f.getString("timezone")
                }.build()
            },
        )
        add(
            healthRecordDefinition(
                "health_record_sleep",
                "health.record.sleep",
                "connect_huawei_health_record_sleep",
            ) { f, start, end, received ->
                HuaweiHealthRecordSleep.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    fallAsleepTime = f.getLong("fall_asleep_time")
                    wakeupTime = f.getLong("wakeup_time")
                    lightSleepTime = f.getInt("light_sleep_time")
                    deepSleepTime = f.getInt("deep_sleep_time")
                    dreamTime = f.getInt("dream_time")
                    awakeTime = f.getInt("awake_time")
                    allSleepTime = f.getInt("all_sleep_time")
                    wakeupCount = f.getInt("wakeup_count")
                    deepSleepPart = f.getInt("deep_sleep_part")
                    sleepScore = f.getInt("sleep_score")
                    sleepLatency = f.getInt("sleep_latency")
                    sleepEfficiency = f.getInt("sleep_efficiency")
                    goBedTime = f.getLong("go_bed_time")
                    sleepType = f.getInt("sleep_type")
                    prepareSleepTime = f.getLong("prepare_sleep_time")
                    offBedTime = f.getLong("off_bed_time")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "heart_rate_variability",
                "heart_rate_variability",
                "connect_huawei_heart_rate_variability",
            ) { f, start, _, received ->
                HuaweiHeartRateVariability.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    heartRateVariabilityRmssd = f.getInt("heart_rate_variability_rmssd")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "resting_calories_statistics",
                "resting_calories.statistics",
                "connect_huawei_resting_calories_statistics",
            ) { f, start, end, received ->
                HuaweiRestingCaloriesStatistics.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    predictedCalories = f.getFloat("predicted_calories")
                    totalCalories = f.getFloat("total_calories")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "sleep_on_off_bed_record",
                "sleep.on_off_bed_record",
                "connect_huawei_sleep_on_off_bed_record",
            ) { f, start, _, received ->
                HuaweiSleepOnOffBedRecord.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    onOffBedState = f.getInt("on_off_bed_state")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "sleep_respiratory_detail",
                "sleep_respiratory_detail",
                "connect_huawei_sleep_respiratory_detail",
            ) { f, start, end, received ->
                HuaweiSleepRespiratoryDetail.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    type = f.getInt("type")
                    value = f.getDouble("value")
                }.build()
            },
        )
        add(
            sampleSetDefinition(
                "sleep_respiratory_event",
                "sleep_respiratory_event",
                "connect_huawei_sleep_respiratory_event",
            ) { f, start, end, received ->
                HuaweiSleepRespiratoryEvent.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    endTime = end?.toEpoch()
                    eventname = f.getInt("event_name")
                }.build()
            },
        )

        add(
            sampleSetDefinition(
                "vo2max",
                "vo2max",
                "connect_huawei_vo2max",
            ) { f, start, _, received ->
                HuaweiVo2Max.newBuilder().apply {
                    time = start.toEpoch()
                    timeReceived = received.toEpoch()
                    vo2max = f.getInt("vo2max")
                }.build()
            },
        )
    }

    private fun FieldValues.toActiveHours(
        start: Instant,
        end: Instant?,
        received: Instant,
    ): HuaweiActiveHours = HuaweiActiveHours.newBuilder().apply {
        time = start.toEpoch()
        timeReceived = received.toEpoch()
        endTime = end?.toEpoch()
        activeHours = getInt("active_hours")
        moderateIntensityMinutes = getInt("moderate_intensity_minutes")
        highIntensityMinutes = getInt("high_intensity_minutes")
    }.build()

    private fun FieldValues.toContinuousActivityStatistics(
        start: Instant,
        end: Instant?,
        received: Instant,
    ): HuaweiContinuousActivityStatistics = HuaweiContinuousActivityStatistics.newBuilder().apply {
        time = start.toEpoch()
        timeReceived = received.toEpoch()
        endTime = end?.toEpoch()
        typeOfActivity = getInt("activity_type")
        span = getInt("span")
        fragments = getInt("fragments")
    }.build()

    private fun FieldValues.toContinuousExerciseIntensity(
        start: Instant,
        end: Instant?,
        received: Instant,
    ): HuaweiContinuousExerciseIntensity = HuaweiContinuousExerciseIntensity.newBuilder().apply {
        time = start.toEpoch()
        timeReceived = received.toEpoch()
        endTime = end?.toEpoch()
        intensity = getDouble("intensity")
        span = getInt("span")
    }.build()

    private fun FieldValues.toHealthRecordHeartRateAlert(
        start: Instant,
        end: Instant?,
        received: Instant,
    ): HuaweiHealthRecordHeartRateAlert = HuaweiHealthRecordHeartRateAlert.newBuilder().apply {
        time = start.toEpoch()
        timeReceived = received.toEpoch()
        endTime = end?.toEpoch()
        threshold = getDouble("threshold")
        avgHeartRate = getDouble("avg_heart_rate")
        maxHeartRate = getDouble("max_heart_rate")
        minHeartRate = getDouble("min_heart_rate")
    }.build()

    /**
     * The 24h ambulatory blood pressure monitoring record has ~80 numeric fields, all following
     * the same `<stat><Metric><Period>` naming (e.g. `avgSystolicBpAll`, `maxHeartRateWake`).
     * [snake] derives each Huawei field key mechanically from the Avro field name to avoid
     * hand-transcribing ~80 near-identical key strings.
     */
    private fun FieldValues.toHealthRecordDynamicBp(
        start: Instant,
        end: Instant?,
        received: Instant,
    ): HuaweiHealthRecordDynamicBp {
        val f = this
        fun i(name: String) = f.getInt(snake(name))
        fun d(name: String) = f.getDouble(snake(name))
        fun l(name: String) = f.getLong(snake(name))
        return HuaweiHealthRecordDynamicBp.newBuilder().apply {
            time = start.toEpoch()
            timeReceived = received.toEpoch()
            endTime = end?.toEpoch()
            planId = f.getString(snake("planId"))
            planStartTime = l("planStartTime")
            planEndTime = l("planEndTime")
            planActualTime = l("planActualTime")
            planStatus = i("planStatus")
            gasBagType = i("gasBagType")
            sleepStartTime = l("sleepStartTime")
            sleepEndTime = l("sleepEndTime")

            validCntAll = i("validCntAll")
            cntAll = i("cntAll")
            maxSystolicBpAll = i("maxSystolicBpAll")
            maxDiastolicBpAll = i("maxDiastolicBpAll")
            maxHeartRateAll = i("maxHeartRateAll")
            midSystolicBpAll = i("midSystolicBpAll")
            midDiastolicBpAll = i("midDiastolicBpAll")
            midHeartRateAll = i("midHeartRateAll")
            minSystolicBpAll = i("minSystolicBpAll")
            minDiastolicBpAll = i("minDiastolicBpAll")
            minHeartRateAll = i("minHeartRateAll")
            avgSystolicBpAll = i("avgSystolicBpAll")
            avgDiastolicBpAll = i("avgDiastolicBpAll")
            avgHeartRateAll = i("avgHeartRateAll")
            stdSystolicBpAll = i("stdSystolicBpAll")
            stdDiastolicBpAll = i("stdDiastolicBpAll")
            stdHeartRateAll = i("stdHeartRateAll")
            coefSystolicBpAll = d("coefSystolicBpAll")
            coefDiastolicBpAll = d("coefDiastolicBpAll")
            coefHeartRateAll = d("coefHeartRateAll")
            loadSystolicBpAll = d("loadSystolicBpAll")
            loadDiastolicBpAll = d("loadDiastolicBpAll")
            dropSystolicBpAll = d("dropSystolicBpAll")
            dropDiastolicBpAll = d("dropDiastolicBpAll")
            peakSystolicBpAll = i("peakSystolicBpAll")
            peakDiastolicBpAll = i("peakDiastolicBpAll")

            validCntWake = i("validCntWake")
            cntWake = i("cntWake")
            maxSystolicBpWake = i("maxSystolicBpWake")
            maxDiastolicBpWake = i("maxDiastolicBpWake")
            maxHeartRateWake = i("maxHeartRateWake")
            midSystolicBpWake = i("midSystolicBpWake")
            midDiastolicBpWake = i("midDiastolicBpWake")
            midHeartRateWake = i("midHeartRateWake")
            minSystolicBpWake = i("minSystolicBpWake")
            minDiastolicBpWake = i("minDiastolicBpWake")
            minHeartRateWake = i("minHeartRateWake")
            avgSystolicBpWake = i("avgSystolicBpWake")
            avgDiastolicBpWake = i("avgDiastolicBpWake")
            avgHeartRateWake = i("avgHeartRateWake")
            stdSystolicBpWake = i("stdSystolicBpWake")
            stdDiastolicBpWake = i("stdDiastolicBpWake")
            stdHeartRateWake = i("stdHeartRateWake")
            coefSystolicBpWake = d("coefSystolicBpWake")
            coefDiastolicBpWake = d("coefDiastolicBpWake")
            coefHeartRateWake = d("coefHeartRateWake")
            loadSystolicBpWake = d("loadSystolicBpWake")
            loadDiastolicBpWake = d("loadDiastolicBpWake")

            validCntSleep = i("validCntSleep")
            cntSleep = i("cntSleep")
            maxSystolicBpSleep = i("maxSystolicBpSleep")
            maxDiastolicBpSleep = i("maxDiastolicBpSleep")
            maxHeartRateSleep = i("maxHeartRateSleep")
            midSystolicBpSleep = i("midSystolicBpSleep")
            midDiastolicBpSleep = i("midDiastolicBpSleep")
            midHeartRateSleep = i("midHeartRateSleep")
            minSystolicBpSleep = i("minSystolicBpSleep")
            minDiastolicBpSleep = i("minDiastolicBpSleep")
            minHeartRateSleep = i("minHeartRateSleep")
            avgSystolicBpSleep = i("avgSystolicBpSleep")
            avgDiastolicBpSleep = i("avgDiastolicBpSleep")
            avgHeartRateSleep = i("avgHeartRateSleep")
            stdSystolicBpSleep = i("stdSystolicBpSleep")
            stdDiastolicBpSleep = i("stdDiastolicBpSleep")
            stdHeartRateSleep = i("stdHeartRateSleep")
            coefSystolicBpSleep = d("coefSystolicBpSleep")
            coefDiastolicBpSleep = d("coefDiastolicBpSleep")
            coefHeartRateSleep = d("coefHeartRateSleep")
            loadSystolicBpSleep = d("loadSystolicBpSleep")
            loadDiastolicBpSleep = d("loadDiastolicBpSleep")

            validCntWakeTwo = i("validCntWakeTwo")
            cntWakeTwo = i("cntWakeTwo")
            maxSystolicBpWakeTwo = i("maxSystolicBpWakeTwo")
            maxDiastolicBpWakeTwo = i("maxDiastolicBpWakeTwo")
            maxHeartRateWakeTwo = i("maxHeartRateWakeTwo")
            midSystolicBpWakeTwo = i("midSystolicBpWakeTwo")
            midDiastolicBpWakeTwo = i("midDiastolicBpWakeTwo")
            midHeartRateWakeTwo = i("midHeartRateWakeTwo")
            minSystolicBpWakeTwo = i("minSystolicBpWakeTwo")
            minDiastolicBpWakeTwo = i("minDiastolicBpWakeTwo")
            minHeartRateWakeTwo = i("minHeartRateWakeTwo")
            avgSystolicBpWakeTwo = i("avgSystolicBpWakeTwo")
            avgDiastolicBpWakeTwo = i("avgDiastolicBpWakeTwo")
            avgHeartRateWakeTwo = i("avgHeartRateWakeTwo")
            stdSystolicBpWakeTwo = i("stdSystolicBpWakeTwo")
            stdDiastolicBpWakeTwo = i("stdDiastolicBpWakeTwo")
            stdHeartRateWakeTwo = i("stdHeartRateWakeTwo")
            coefSystolicBpWakeTwo = d("coefSystolicBpWakeTwo")
            coefDiastolicBpWakeTwo = d("coefDiastolicBpWakeTwo")
            coefHeartRateWakeTwo = d("coefHeartRateWakeTwo")

            extendData = f.getString("extend_data")
        }.build()
    }

    private fun sampleSetDefinition(
        key: String,
        dataTypeSuffix: String,
        defaultTopic: String,
        buildRecord: (
            fields: FieldValues,
            startTime: Instant,
            endTime: Instant?,
            timeReceived: Instant,
        ) -> SpecificRecord,
    ): HuaweiRouteDefinition = HuaweiRouteDefinition(key, defaultTopic) { repo, topic ->
        HuaweiSampleSetRoute(
            userRepository = repo,
            // Huawei's polymerize API has no dataCollector for a literal "*.statistics" data
            // type - ".statistics" is only this connector's/RADAR-Schemas' label for "the
            // groupByTime-aggregated variant of the underlying raw data type", so it must be
            // stripped from the dataTypeName actually sent on the wire.
            dataTypeName = VENDOR_PREFIX + dataTypeSuffix.removeSuffix(".statistics"),
            topic = topic,
            groupByTimeUnit = if (dataTypeSuffix.endsWith(".statistics")) "day" else null,
            buildRecord = buildRecord,
        )
    }

    private fun healthRecordDefinition(
        key: String,
        dataTypeName: String,
        defaultTopic: String,
        buildRecord: (
            fields: FieldValues,
            startTime: Instant,
            endTime: Instant?,
            timeReceived: Instant,
        ) -> SpecificRecord,
    ): HuaweiRouteDefinition = HuaweiRouteDefinition(key, defaultTopic) { repo, topic ->
        HuaweiHealthRecordRoute(
            userRepository = repo,
            dataTypeName = VENDOR_PREFIX + dataTypeName,
            topic = topic,
            buildRecord = buildRecord,
        )
    }
}
