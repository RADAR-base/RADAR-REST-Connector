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

package org.radarbase.huawei.converter

import org.apache.avro.specific.SpecificRecord

/** Single value for a topic.  */
/**
 * @author yatharthranjan
 */
data class TopicData(
    val topic: String,
    val key: SpecificRecord,
    val value: SpecificRecord,
    val offset: Long,
)
