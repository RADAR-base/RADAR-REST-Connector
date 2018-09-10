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

package org.radarbase.connect.rest.request;

import okhttp3.Response;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;

/** Single request route. This may represent e.g. a URL. */
public interface RequestRoute extends RequestGenerator {
  /** Data converter from data that is returned from the route. */
  PayloadToSourceRecordConverter converter();

  /**
   * Called when the request from this route succeeded.
   *
   * @param request non-null generated request
   * @param record non-null resulting record
   */
  void requestSucceeded(RestRequest request, SourceRecord record);
  void requestEmpty(RestRequest request);
  void requestFailed(RestRequest request, Response response);
}
