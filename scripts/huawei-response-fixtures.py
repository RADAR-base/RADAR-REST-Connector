#!/usr/bin/env python3
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
# Turns responses saved by huawei-api-probe.sh into test fixtures for HuaweiRecordedResponseTest:
#   ./huawei-response-fixtures.py <probe OUT_DIR> \
#     ../huawei-library/src/test/resources/huawei-responses [route key to skip ...]
#
# Only responses with data are written. The envelope and every value type stay exactly as Huawei
# sent them; lists are trimmed and device- or account-specific identifiers are replaced. Review the
# output before committing it: the remaining values are still the recording user's health data.
import json, os, re, sys

src, dst = sys.argv[1], sys.argv[2]
skip = set(sys.argv[3:])
OPAQUE = re.compile(r"^[A-Za-z0-9_\-+/=]{40,}$")

def first(d, n):
    return dict(list(d.items())[:n])

def scrub(o, key=None):
    if isinstance(o, dict):
        out = {}
        for k, v in o.items():
            if k == "uniqueId":
                v = "device-unique-id"
            elif k == "clientId":
                v = "client-id"
            elif k in ("dataCollectorId", "originalDataCollectorId") and isinstance(v, str) and OPAQUE.match(v):
                v = "opaque-data-collector-id"
            elif k == "id" and isinstance(v, str) and OPAQUE.match(v):
                v = "health-record-id"  # encodes the device serial number
            elif o.get("fieldName") == "voltage_datas" and k == "stringValue":
                v = "[" + ",".join(v.strip("[]").split(",")[:5]) + "]"
            elif k in ("paceMap", "sportHealthPaceMap", "partTimeMap"):
                v = first(v, 2)
            elif k in ("sectionSummary",):
                v = v[:1]
            elif k == "samplePoints":
                v = v[:2]
            elif k in ("activityRecord", "healthRecords"):
                v = v[:1]
            out[k] = scrub(v, k)
        return out
    if isinstance(o, list):
        return [scrub(x, key) for x in o]
    return o

os.makedirs(dst, exist_ok=True)
for f in sorted(os.listdir(src)):
    if not f.endswith(".json") or f[:-5] in skip:
        continue
    data = json.load(open(os.path.join(src, f)))
    if "error" in data or '"value"' not in json.dumps(data):
        continue
    with open(os.path.join(dst, f), "w") as out:
        json.dump(scrub(data), out, indent=2)
        out.write("\n")
    print(f[:-5])
