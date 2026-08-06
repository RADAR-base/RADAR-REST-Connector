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

package org.radarbase.connect.rest.huawei

import org.junit.jupiter.api.Test
import org.radarbase.huawei.route.HuaweiRouteFactory
import kotlin.test.assertEquals

/**
 * @author yatharthranjan
 */
class HuaweiRestSourceConnectorConfigTest {

    @Test
    fun conf() {
        println(HuaweiRestSourceConnectorConfig.conf().toHtmlTable())
    }

    @Test
    fun `enabled topics default to every registered data type`() {
        val config = HuaweiRestSourceConnectorConfig(
            mutableMapOf(
                "huawei.api.client" to "client",
                "huawei.api.secret" to "secret",
            ),
            false,
        )

        val enabled = config.enabledTopics()

        assertEquals(HuaweiRouteFactory.definitions.size, enabled.size)
        HuaweiRouteFactory.definitions.forEach { definition ->
            assertEquals(definition.defaultTopic, enabled[definition.key])
        }
    }

    @Test
    fun `a data type can be disabled via config`() {
        val definition = HuaweiRouteFactory.definitions.first()
        val config = HuaweiRestSourceConnectorConfig(
            mutableMapOf(
                "huawei.api.client" to "client",
                "huawei.api.secret" to "secret",
                "huawei.${definition.key}.enabled" to "false",
            ),
            false,
        )

        assertEquals(null, config.enabledTopics()[definition.key])
    }
}
