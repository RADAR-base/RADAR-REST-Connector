#!/usr/bin/env bash
#
# Copyright 2026 Onsentia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author yatharthranjan
#
# Queries every Huawei Health Kit endpoint exactly as the RADAR Huawei connector does, saving
# each response (with its HTTP status) to $OUT_DIR/<route key>.json for review.
#
# Authenticate with EITHER a refresh token plus your Huawei app's client credentials (a new
# access token is fetched first):
#   HUAWEI_CLIENT_ID=<app id> HUAWEI_CLIENT_SECRET=<app secret> \
#   HUAWEI_REFRESH_TOKEN=<refresh token> ./huawei-api-probe.sh
# OR a still-valid access token:
#   HUAWEI_TOKEN=<access token> ./huawei-api-probe.sh
#
# Instead of HUAWEI_REFRESH_TOKEN you can pass USER_FILE=docker/users/<user>.yml to read
# oauth2.refreshToken from the connector's user file (the file is not modified).
#
# The route list below mirrors org.radarbase.huawei.route.HuaweiRouteFactory; keep it in sync when
# routes are added or changed. Responses contain health data: don't commit or share them untrimmed.
#
# Options:
#   DAYS=30 ./huawei-api-probe.sh                          # window size, max 30 (default 7)
#   ./huawei-api-probe.sh continuous_ecg_detail emotion    # only these routes
set -euo pipefail

TOKEN_URL=https://oauth-login.cloud.huawei.com/oauth2/v3/token

json_field() {  # json key -> string/number value of a top-level key, or empty
  if command -v python3 >/dev/null; then
    python3 -c 'import json,sys; v=json.loads(sys.argv[1]).get(sys.argv[2]); print("" if v is None else v)' "$1" "$2"
  elif command -v jq >/dev/null; then
    jq -r --arg k "$2" '.[$k] // empty' <<<"$1"
  else
    sed -n "s/.*\"$2\"[[:space:]]*:[[:space:]]*\"\{0,1\}\([^\",}]*\).*/\1/p" <<<"$1"
  fi
}

if [ -z "${HUAWEI_REFRESH_TOKEN:-}" ] && [ -n "${USER_FILE:-}" ]; then
  # First non-empty "refreshToken:" value in the YAML file, without surrounding quotes.
  HUAWEI_REFRESH_TOKEN=$(sed -n 's/^[[:space:]]*refreshToken:[[:space:]]*//p' "$USER_FILE" \
    | head -n 1 | sed -e 's/[[:space:]]*$//' -e 's/^["'\'']//' -e 's/["'\'']$//')
  [ -n "$HUAWEI_REFRESH_TOKEN" ] || { echo "No refreshToken found in $USER_FILE" >&2; exit 1; }
fi

if [ -n "${HUAWEI_REFRESH_TOKEN:-}" ]; then
  : "${HUAWEI_CLIENT_ID:?Set HUAWEI_CLIENT_ID (your Huawei app ID, huawei.api.client in the connector config)}"
  : "${HUAWEI_CLIENT_SECRET:?Set HUAWEI_CLIENT_SECRET (huawei.api.secret in the connector config)}"
  echo "Refreshing access token..."
  TOKEN_RESPONSE=$(curl -sS -w '\n%{http_code}' -X POST "$TOKEN_URL" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode grant_type=refresh_token \
    --data-urlencode "client_id=$HUAWEI_CLIENT_ID" \
    --data-urlencode "client_secret=$HUAWEI_CLIENT_SECRET" \
    --data-urlencode "refresh_token=$HUAWEI_REFRESH_TOKEN")
  TOKEN_STATUS=${TOKEN_RESPONSE##*$'\n'}
  TOKEN_BODY=${TOKEN_RESPONSE%$'\n'*}
  if [ "$TOKEN_STATUS" != 200 ]; then
    echo "Token refresh failed (HTTP $TOKEN_STATUS): $TOKEN_BODY" >&2
    exit 1
  fi
  HUAWEI_TOKEN=$(json_field "$TOKEN_BODY" access_token)
  [ -n "$HUAWEI_TOKEN" ] || { echo "No access_token in token response: $TOKEN_BODY" >&2; exit 1; }
  echo "Got access token (expires in $(json_field "$TOKEN_BODY" expires_in)s, scopes: $(json_field "$TOKEN_BODY" scope))"
  NEW_REFRESH_TOKEN=$(json_field "$TOKEN_BODY" refresh_token)
  if [ -n "$NEW_REFRESH_TOKEN" ] && [ "$NEW_REFRESH_TOKEN" != "$HUAWEI_REFRESH_TOKEN" ]; then
    echo >&2
    echo "WARNING: Huawei issued a NEW refresh token. The old one may no longer work, so put this" >&2
    echo "one in oauth2.refreshToken of your user file before (re)starting the connector:" >&2
    echo "$NEW_REFRESH_TOKEN" >&2
    echo >&2
  fi
fi

: "${HUAWEI_TOKEN:?Set HUAWEI_REFRESH_TOKEN (+ HUAWEI_CLIENT_ID/SECRET), USER_FILE, or HUAWEI_TOKEN}"
DAYS="${DAYS:-7}"
OUT_DIR="${OUT_DIR:-huawei-responses}"
V1=https://health-api.cloud.huawei.com/healthkit/v1
V2=https://health-api.cloud.huawei.com/healthkit/v2

# Window: the last DAYS complete UTC days, [START, END).
END_S=$(( $(date -u +%s) / 86400 * 86400 ))
START_S=$(( END_S - DAYS * 86400 ))
utc_day() { date -u -d "@$1" +%Y%m%d 2>/dev/null || date -u -r "$1" +%Y%m%d; }  # GNU || BSD/macOS
START_MS=${START_S}000
END_MS=${END_S}000
START_NS=${START_S}000000000
END_NS=${END_S}000000000
START_DAY=$(utc_day "$START_S")
END_DAY=$(utc_day "$(( END_S - 86400 ))")  # endDay is inclusive

mkdir -p "$OUT_DIR"
echo "Window: $START_DAY .. $END_DAY (UTC), saving to $OUT_DIR/"

wanted() { [ $# -eq 0 ] && return 0; local k; for k in $ONLY; do [ "$k" = "$1" ] && return 0; done; return 1; }
ONLY="$*"

call() {  # key method url [json body]
  local key=$1 method=$2 url=$3 body=${4:-}
  [ -n "$ONLY" ] && ! wanted "$key" && return 0
  local args=(-sS -o "$OUT_DIR/$key.json" -w '%{http_code}' -X "$method" "$url"
    -H "Authorization: Bearer $HUAWEI_TOKEN" -H 'Content-Type: application/json;charset=UTF-8')
  [ -n "$body" ] && args+=(--data "$body")
  local status; status=$(curl "${args[@]}" || echo "curl-failed")
  if [ "$status" = 200 ]; then
    printf '%-45s %s\n' "$key" "$status"
  else
    # Error bodies contain no health data; show them inline for easy sharing.
    printf '%-45s %s %s\n' "$key" "$status" "$(head -c 300 "$OUT_DIR/$key.json" 2>/dev/null | tr -d '\n')"
  fi
  sleep 1  # stay well clear of rate limits
}

polymerize() {  # key dataTypeName
  local body
  body=$(printf '{"polymerizeWith":[{"dataTypeName":"%s"}],"startTime":%s,"endTime":%s}' \
    "$2" "$START_MS" "$END_MS")
  call "$1" POST "$V1/sampleSet:polymerize" "$body"
}

daily() {  # key dataTypeName
  local body
  body=$(printf '{"dataTypes":["%s"],"startDay":"%s","endDay":"%s","timeZone":"+0000"}' \
    "$2" "$START_DAY" "$END_DAY")
  call "$1" POST "$V2/sampleSet:dailyPolymerize" "$body"
}

health_record() {  # key dataType [subDataType]
  local url="$V2/healthRecords?dataType=$2&startTime=$START_NS&endTime=$END_NS"
  [ -n "${3:-}" ] && url="$url&subDataType=$3"
  call "$1" GET "$url"
}

call activity_record GET "$V2/activityRecords?startTime=$START_MS&endTime=$END_MS"


# Raw sample points: POST v1 sampleSet:polymerize (times in ms)
polymerize cgm_blood_glucose com.huawei.cgm_blood_glucose
polymerize active_hours com.huawei.active_hours
polymerize continuous_activity_fragment com.huawei.continuous.activity.fragment
polymerize instantaneous_body_temperature com.huawei.instantaneous.body.temperature
polymerize instantaneous_skin_temperature com.huawei.instantaneous.skin.temperature
polymerize continuous_calories_burnt com.huawei.continuous.calories.burnt
polymerize continuous_calories_consumed com.huawei.continuous.calories.consumed  # disabled by default
polymerize continuous_distance_delta com.huawei.continuous.distance.delta
polymerize continuous_exercise_intensity com.huawei.continuous.exercise_intensity
polymerize continuous_exercise_intensity_v2 com.huawei.continuous.exercise_intensity.v2
polymerize continuous_sleep_fragment com.huawei.continuous.sleep.fragment
polymerize continuous_steps_delta com.huawei.continuous.steps.delta
polymerize emotion com.huawei.emotion
polymerize heart_rate_variability com.huawei.heart_rate_variability
polymerize sleep_on_off_bed_record com.huawei.sleep.on_off_bed_record
polymerize sleep_respiratory_detail com.huawei.sleep_respiratory_detail
polymerize sleep_respiratory_event com.huawei.sleep_respiratory_event
polymerize vo2max com.huawei.vo2max

# Daily statistics: POST v2 sampleSet:dailyPolymerize (the raw type is queried)
daily cgm_blood_glucose_statistics com.huawei.cgm_blood_glucose
daily daily_activity_summary com.huawei.daily_activity_summary
daily active_hours_statistics com.huawei.active_hours
daily continuous_activity_statistics com.huawei.continuous.activity
daily continuous_altitude_statistics com.huawei.instantaneous.altitude
daily continuous_blood_glucose_statistics com.huawei.instantaneous.blood_glucose
daily continuous_breathe_rate_statistics com.huawei.instantaneous.breathe_rate
daily continuous_body_blood_pressure_statistics com.huawei.instantaneous.blood_pressure
daily continuous_body_temperature_rest_statistics com.huawei.continuous.body.temperature.rest  # disabled by default
daily continuous_calories_bmr_statistics com.huawei.continuous.calories.bmr
daily continuous_exercise_heart_rate_statistics com.huawei.continuous.exercise_heart_rate
daily continuous_power_statistics com.huawei.continuous.power
daily continuous_speed_statistics com.huawei.continuous.speed
daily continuous_steps_rate_statistics com.huawei.continuous.steps.rate
daily continuous_stroke_rate_statistics com.huawei.continuous.stroke_rate
daily instantaneous_resting_heart_rate_statistics com.huawei.instantaneous.resting_heart_rate
daily instantaneous_stress_statistics com.huawei.instantaneous.stress
daily vo2max_statistics com.huawei.vo2max
daily continuous_heart_rate_statistics com.huawei.instantaneous.heart_rate
daily continuous_body_temperature_statistics com.huawei.instantaneous.body.temperature
daily continuous_skin_temperature_statistics com.huawei.instantaneous.skin.temperature
daily continuous_body_fat_rate_statistics com.huawei.instantaneous.body_weight
daily continuous_calories_burnt_total com.huawei.continuous.calories.burnt
daily continuous_distance_total com.huawei.continuous.distance.delta
daily continuous_exercise_intensity_statistics com.huawei.continuous.exercise_intensity
daily continuous_exercise_intensity_v2_statistics com.huawei.continuous.exercise_intensity.v2
daily continuous_spo2_statistics com.huawei.instantaneous.spo2
daily continuous_steps_total com.huawei.continuous.steps.delta
daily resting_calories_statistics com.huawei.resting_calories

# Health records: GET v2 healthRecords (times in ns)
health_record continuous_ecg_detail com.huawei.continuous.ecg_record com.huawei.continuous.ecg_detail
health_record health_record_dynamic_bp com.huawei.health.record.dynamic_bp
health_record health_record_bradycardia com.huawei.health.record.bradycardia
health_record health_record_tachycardia com.huawei.health.record.tachycardia
health_record health_record_hyperthermia com.huawei.health.record.hyperthermia
health_record health_record_low_spo2_alert com.huawei.health.record.lowSpo2Alert
health_record health_record_menstrual_cycle com.huawei.health.record.menstrual_cycle
health_record health_record_sleep com.huawei.health.record.sleep
