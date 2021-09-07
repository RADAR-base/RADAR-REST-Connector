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

package org.radarbase.connect.rest.converter;

import static java.lang.System.currentTimeMillis;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import okhttp3.Headers;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.request.RestRequest;
import org.radarbase.connect.rest.selector.TopicSelector;

public class BytesPayloadConverter implements PayloadToSourceRecordConverter {
  private TopicSelector topicSelector;

  // Just bytes for incoming messages
  @Override
  public Collection<SourceRecord> convert(RestRequest request, Headers headers, byte[] data) {
    Map<String, Long> sourceOffset = Collections.singletonMap(
        TIMESTAMP_OFFSET_KEY, currentTimeMillis());
    String topic = topicSelector.getTopic(request, data);
    return Collections.singleton(
        new SourceRecord(request.getPartition(), sourceOffset,
            topic, Schema.BYTES_SCHEMA, data));
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    topicSelector = config.getTopicSelector();
  }
}
