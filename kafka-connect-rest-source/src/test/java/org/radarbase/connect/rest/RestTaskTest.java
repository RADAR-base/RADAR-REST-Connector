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

package org.radarbase.connect.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.radarbase.connect.rest.RestTaskTest.WireMockRule;
import org.radarbase.connect.rest.converter.BytesPayloadConverter;
import org.radarbase.connect.rest.converter.StringPayloadConverter;
import org.radarbase.connect.rest.selector.SimpleTopicSelector;
import org.radarbase.connect.rest.single.SingleRestSourceConnector;
import org.radarbase.connect.rest.single.SingleRestSourceConnectorConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(WireMockRule.class)
public class RestTaskTest {

  private static final String CONTENT_TYPE = "Content-Type";
  private static final String ACCEPT = "Accept";
  private static final String APPLICATION_JSON = "application/json; charset=UTF-8";

  private static final String TOPIC = "rest-source-destination-topic";
  private static final String REST_SOURCE_DESTINATION_TOPIC_LIST = TOPIC;

  private static final String METHOD = "POST";
  private static final String PROPERTIES_LIST = "" +
    CONTENT_TYPE + ":" + APPLICATION_JSON + ", " +
    ACCEPT + ":" + APPLICATION_JSON;
  private static final String TOPIC_SELECTOR = SimpleTopicSelector.class.getName();
  private static final String BYTES_PAYLOAD_CONVERTER = BytesPayloadConverter.class.getName();
  private static final String STRING_PAYLOAD_CONVERTER = StringPayloadConverter.class.getName();
  private static final String DATA = "{\"A\":\"B\"}";
  private static final String RESPONSE_BODY = "{\"B\":\"A\"}";
  private static final String PATH = "/my/resource";

  @Test
  public void restTest(WireMockRule wireMock) throws InterruptedException {
    stubFor(post(urlEqualTo(PATH))
      .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(RESPONSE_BODY)));

    Map<String, String> props;
    props = new HashMap<>();
    props.put("connector.class", SingleRestSourceConnector.class.getName());
    props.put(SingleRestSourceConnectorConfig.SOURCE_METHOD_CONFIG, METHOD);
    props.put(SingleRestSourceConnectorConfig.SOURCE_PROPERTIES_LIST_CONFIG, PROPERTIES_LIST);
    props.put(RestSourceConnectorConfig.SOURCE_URL_CONFIG, wireMock.url(PATH));
    props.put(SingleRestSourceConnectorConfig.SOURCE_DATA_CONFIG, DATA);
    props.put(RestSourceConnectorConfig.SOURCE_TOPIC_SELECTOR_CONFIG, TOPIC_SELECTOR);
    props.put(RestSourceConnectorConfig.SOURCE_TOPIC_LIST_CONFIG, REST_SOURCE_DESTINATION_TOPIC_LIST);
    props.put(RestSourceConnectorConfig.SOURCE_PAYLOAD_CONVERTER_CONFIG, STRING_PAYLOAD_CONVERTER);


    RestSourceTask sourceTask;
    List<SourceRecord> messages;

    sourceTask = new RestSourceTask();
    SourceTaskContext context = new SourceTaskContext() {
      @Override
      public Map<String, String> configs() {
        return props;
      }

      @Override
      public OffsetStorageReader offsetStorageReader() {
        return null;
      }
    };

    sourceTask.initialize(context);
    sourceTask.start(props);
    messages = sourceTask.poll();

    assertEquals(1, messages.size(), "Message count: ");
    assertEquals(String.class, messages.get(0).value().getClass(), "Response class: ");
    assertEquals(RESPONSE_BODY, messages.get(0).value(), "Response body: ");
    assertEquals(TOPIC, messages.get(0).topic(), "Topic: ");

    verify(postRequestedFor(urlMatching(PATH))
      .withRequestBody(equalTo(DATA))
      .withHeader(CONTENT_TYPE, matching(APPLICATION_JSON)));

    props.put(RestSourceConnectorConfig.SOURCE_PAYLOAD_CONVERTER_CONFIG, BYTES_PAYLOAD_CONVERTER);

    sourceTask = new RestSourceTask();
    sourceTask.initialize(context);
    sourceTask.start(props);
    messages = sourceTask.poll();

    assertEquals(1, messages.size(), "Message count: ");
    assertEquals(byte[].class, messages.get(0).value().getClass(), "Response class: ");
    assertEquals(RESPONSE_BODY, new String((byte[]) messages.get(0).value()), "Response body: ");
    assertEquals(TOPIC, messages.get(0).topic(), "Topic: ");

    verify(postRequestedFor(urlMatching(PATH))
      .withRequestBody(equalTo(DATA))
      .withHeader(CONTENT_TYPE, matching(APPLICATION_JSON)));
  }

  public static class WireMockRule extends WireMockServer implements BeforeEachCallback,
      AfterEachCallback, ParameterResolver {

    public WireMockRule() {
      super(wireMockConfig().dynamicPort());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
      start();
      WireMock.configureFor("localhost", port());
    }

    @Override
    public void afterEach(ExtensionContext context) {
      try {
        checkForUnmatchedRequests();
      } finally {
        resetAllRequests();
        stop();
      }
    }

    public void checkForUnmatchedRequests() {
      List<LoggedRequest> unmatchedRequests = findAllUnmatchedRequests();
      if (!unmatchedRequests.isEmpty()) {
        List<NearMiss> nearMisses = findNearMissesForAllUnmatchedRequests();
        if (nearMisses.isEmpty()) {
          throw VerificationException.forUnmatchedRequests(unmatchedRequests);
        } else {
          throw VerificationException.forUnmatchedNearMisses(nearMisses);
        }
      }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
        ExtensionContext extensionContext) throws ParameterResolutionException {
      return parameterContext.getParameter().getType() == WireMockRule.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
        ExtensionContext extensionContext) throws ParameterResolutionException {
      return this;
    }
  }
}
