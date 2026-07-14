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
 */
data class HuaweiRouteDefinition(
    val key: String,
    val defaultTopic: String,
    val enabledByDefault: Boolean = true,
    val build: (UserRepository, topic: String) -> HuaweiRoute,
)
