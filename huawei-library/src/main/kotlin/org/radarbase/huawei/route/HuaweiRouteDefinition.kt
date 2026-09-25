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

import org.radarbase.huawei.user.UserRepository

/**
 * A single registered Huawei Health Kit data type: a short config key (used to build
 * `huawei.<key>.enabled` / `huawei.<key>.topic` connector properties), the default Kafka topic
 * name, and a factory for the [HuaweiRoute] that queries it.
 *
 * Using one shared registry (see [HuaweiRouteFactory]) for both the Kafka Connect config
 * definition and the set of routes actually polled avoids hand-duplicating each of the ~54 Huawei
 * data types across a `ConfigDef` and a route-construction switch.
 *
 * @author yatharthranjan
 */
data class HuaweiRouteDefinition(
    val key: String,
    val defaultTopic: String,
    val enabledByDefault: Boolean = true,
    val build: (UserRepository, topic: String) -> HuaweiRoute,
)
