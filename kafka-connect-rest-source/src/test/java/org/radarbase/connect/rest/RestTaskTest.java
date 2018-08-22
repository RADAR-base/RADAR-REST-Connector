package org.radarbase.connect.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.junit.Rule;
import org.junit.Test;
import org.radarbase.connect.rest.converter.BytesPayloadConverter;
import org.radarbase.connect.rest.converter.StringPayloadConverter;
import org.radarbase.connect.rest.selector.SimpleTopicSelector;
import org.radarbase.connect.rest.single.SingleRestSourceConnector;
import org.radarbase.connect.rest.single.SingleRestSourceConnectorConfig;

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
  private static final int PORT = getPort();
  private static final String PATH = "/my/resource";
  private static final String URL = "http://localhost:" + PORT + PATH;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(PORT);

  @Test
  public void restTest() throws InterruptedException {
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
    props.put(RestSourceConnectorConfig.SOURCE_URL_CONFIG, URL);
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

    assertEquals("Message count: ", 1, messages.size());
    assertEquals("Response class: ", String.class, messages.get(0).value().getClass());
    assertEquals("Response body: ", RESPONSE_BODY, messages.get(0).value());
    assertEquals("Topic: ", TOPIC, messages.get(0).topic());

    verify(postRequestedFor(urlMatching(PATH))
      .withRequestBody(equalTo(DATA))
      .withHeader(CONTENT_TYPE, matching(APPLICATION_JSON)));

    props.put(RestSourceConnectorConfig.SOURCE_PAYLOAD_CONVERTER_CONFIG, BYTES_PAYLOAD_CONVERTER);

    sourceTask = new RestSourceTask();
    sourceTask.initialize(context);
    sourceTask.start(props);
    messages = sourceTask.poll();

    assertEquals("Message count: ", 1, messages.size());
    assertEquals("Response class: ", byte[].class, messages.get(0).value().getClass());
    assertEquals("Response body: ", RESPONSE_BODY, new String((byte[]) messages.get(0).value()));
    assertEquals("Topic: ", TOPIC, messages.get(0).topic());

    verify(postRequestedFor(urlMatching(PATH))
      .withRequestBody(equalTo(DATA))
      .withHeader(CONTENT_TYPE, matching(APPLICATION_JSON)));

    wireMockRule.resetRequests();
  }

  private static int getPort() {
    try {
      ServerSocket s = new ServerSocket(0);
      int localPort = s.getLocalPort();
      s.close();
      return localPort;
    } catch (Exception e) {
      throw new RuntimeException("Failed to get a free PORT", e);
    }
  }
}
