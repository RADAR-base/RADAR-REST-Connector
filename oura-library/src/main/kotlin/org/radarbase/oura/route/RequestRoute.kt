/*
 * Copyright 2018 The Hyve
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
package org.radarbase.oura.route

import okhttp3.Response
import org.radarbase.oura.converter.PayloadToSourceRecordConverter
import org.radarbase.oura.converter.TopicData
import org.radarbase.oura.request.OuraRequest
import org.radarbase.oura.request.OuraRequestGenerator

/**
 * Single request route. This may represent e.g. a URL.
 */
interface RequestRoute: OuraRequestGenerator {

    fun converter(): PayloadToSourceRecordConverter

    /**
     * Called when the request from this route succeeded.
     *
     * @param request non-null generated request
     * @param record  non-null resulting records
     */
    fun requestSucceeded(request: OuraRequest, record: Sequence<Result<TopicData>>)
    fun requestEmpty(request: OuraRequest)
    fun requestFailed(request: OuraRequest, response: Response?)
}