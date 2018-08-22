package org.radarbase.connect.rest;

import static org.apache.kafka.common.config.ConfigDef.NO_DEFAULT_VALUE;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.ConfigKey;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;
import org.apache.kafka.connect.errors.ConnectException;
import org.radarbase.connect.rest.config.ValidClass;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;
import org.radarbase.connect.rest.converter.StringPayloadConverter;
import org.radarbase.connect.rest.request.RequestGenerator;
import org.radarbase.connect.rest.selector.SimpleTopicSelector;
import org.radarbase.connect.rest.selector.TopicSelector;
import org.radarbase.connect.rest.single.SingleRequestGenerator;
import org.radarbase.connect.rest.util.VersionUtil;

public class RestSourceConnectorConfig extends AbstractConfig {
  public static final Pattern COLON_PATTERN = Pattern.compile(":");

  private static final String SOURCE_POLL_INTERVAL_CONFIG = "rest.source.poll.interval.ms";
  private static final String SOURCE_POLL_INTERVAL_DOC = "How often to poll the source URL.";
  private static final String SOURCE_POLL_INTERVAL_DISPLAY = "Polling interval";
  private static final Long SOURCE_POLL_INTERVAL_DEFAULT = 60000L;

  static final String SOURCE_URL_CONFIG = "rest.source.base.url";
  private static final String SOURCE_URL_DOC = "Base URL for REST source connector.";
  private static final String SOURCE_URL_DISPLAY = "Base URL for REST source connector.";

  static final String SOURCE_TOPIC_SELECTOR_CONFIG = "rest.source.topic.selector";
  private static final String SOURCE_TOPIC_SELECTOR_DOC =
      "The topic selector class for REST source connector.";
  private static final String SOURCE_TOPIC_SELECTOR_DISPLAY =
      "Topic selector class for REST source connector.";
  private static final Class<? extends TopicSelector> SOURCE_TOPIC_SELECTOR_DEFAULT =
      SimpleTopicSelector.class;

  public static final String SOURCE_TOPIC_LIST_CONFIG = "rest.source.destination.topics";
  private static final String SOURCE_TOPIC_LIST_DOC =
      "The  list of destination topics for the REST source connector.";
  private static final String SOURCE_TOPIC_LIST_DISPLAY = "Source destination topics";

  public static final String SOURCE_PAYLOAD_CONVERTER_CONFIG = "rest.source.payload.converter.class";
  private static final Class<? extends PayloadToSourceRecordConverter> PAYLOAD_CONVERTER_DEFAULT =
      StringPayloadConverter.class;
  private static final String SOURCE_PAYLOAD_CONVERTER_DOC_CONFIG =
      "Class to be used to convert messages from REST calls to SourceRecords";
  private static final String SOURCE_PAYLOAD_CONVERTER_DISPLAY_CONFIG = "Payload converter class";

  private static final String SOURCE_REQUEST_GENERATOR_CONFIG = "rest.source.request.generator.class";
  private static final Class<? extends RequestGenerator> REQUEST_GENERATOR_DEFAULT =
      SingleRequestGenerator.class;
  private static final String REQUEST_GENERATOR_DOC =
      "Class to be used to generate REST requests";
  private static final String REQUEST_GENERATOR_DISPLAY = "Request generator class";

  private final TopicSelector topicSelector;
  private final PayloadToSourceRecordConverter payloadToSourceRecordConverter;
  private final RequestGenerator requestGenerator;

  @SuppressWarnings("unchecked")
  public RestSourceConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
    super(config, parsedConfig);
    try {
      topicSelector = ((Class<? extends TopicSelector>)
          getClass(SOURCE_TOPIC_SELECTOR_CONFIG)).getDeclaredConstructor().newInstance();
      payloadToSourceRecordConverter = ((Class<? extends PayloadToSourceRecordConverter>)
          getClass(SOURCE_PAYLOAD_CONVERTER_CONFIG)).getDeclaredConstructor().newInstance();
      requestGenerator = ((Class<? extends RequestGenerator>)
          getClass(SOURCE_REQUEST_GENERATOR_CONFIG)).getDeclaredConstructor().newInstance();
    } catch (IllegalAccessException | InstantiationException
        | InvocationTargetException | NoSuchMethodException e) {
      throw new ConnectException("Invalid class for: " + SOURCE_PAYLOAD_CONVERTER_CONFIG, e);
    }
  }

  public RestSourceConnectorConfig(Map<String, String> parsedConfig) {
    this(conf(), parsedConfig);
  }

  public static ConfigDef conf() {
    String group = "REST";
    int orderInGroup = 0;
    return new ConfigDef()
        .define(SOURCE_POLL_INTERVAL_CONFIG,
            Type.LONG,
            SOURCE_POLL_INTERVAL_DEFAULT,
            Importance.LOW,
            SOURCE_POLL_INTERVAL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            SOURCE_POLL_INTERVAL_DISPLAY)

        .define(SOURCE_URL_CONFIG,
            Type.STRING,
            NO_DEFAULT_VALUE,
            Importance.HIGH,
            SOURCE_URL_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            SOURCE_URL_DISPLAY)

        .define(SOURCE_TOPIC_LIST_CONFIG,
            Type.LIST,
            Collections.emptyList(),
            Importance.HIGH,
            SOURCE_TOPIC_LIST_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            SOURCE_TOPIC_LIST_DISPLAY)

        .define(SOURCE_TOPIC_SELECTOR_CONFIG,
            Type.CLASS,
            SOURCE_TOPIC_SELECTOR_DEFAULT,
            ValidClass.isSubclassOf(TopicSelector.class),
            Importance.HIGH,
            SOURCE_TOPIC_SELECTOR_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            SOURCE_TOPIC_SELECTOR_DISPLAY)

        .define(SOURCE_PAYLOAD_CONVERTER_CONFIG,
            Type.CLASS,
            PAYLOAD_CONVERTER_DEFAULT,
            ValidClass.isSubclassOf(PayloadToSourceRecordConverter.class),
            Importance.LOW,
            SOURCE_PAYLOAD_CONVERTER_DOC_CONFIG,
            group,
            ++orderInGroup,
            Width.SHORT,
            SOURCE_PAYLOAD_CONVERTER_DISPLAY_CONFIG)

        .define(SOURCE_REQUEST_GENERATOR_CONFIG,
            Type.CLASS,
            REQUEST_GENERATOR_DEFAULT,
            ValidClass.isSubclassOf(RequestGenerator.class),
            Importance.LOW,
            REQUEST_GENERATOR_DOC,
            group,
            ++orderInGroup,
            Width.SHORT,
            REQUEST_GENERATOR_DISPLAY)
        ;
  }

  public Duration getPollInterval() {
    return Duration.ofMillis(this.getLong(SOURCE_POLL_INTERVAL_CONFIG));
  }

  public String getUrl() {
    return this.getString(SOURCE_URL_CONFIG);
  }

  public List<String> getTopics() {
    return this.getList(SOURCE_TOPIC_LIST_CONFIG);
  }

  public TopicSelector getTopicSelector() {
    topicSelector.initialize(this);
    return topicSelector;
  }

  public PayloadToSourceRecordConverter getPayloadToSourceRecordConverter() {
    payloadToSourceRecordConverter.initialize(this);
    return payloadToSourceRecordConverter;
  }

  private static ConfigDef getConfig() {
    Map<String, ConfigKey> everything = new HashMap<>(conf().configKeys());
    ConfigDef visible = new ConfigDef();
    for (ConfigKey key : everything.values()) {
      visible.define(key);
    }
    return visible;
  }

  public static void main(String[] args) {
    System.out.println(VersionUtil.getVersion());
    System.out.println(getConfig().toEnrichedRst());
  }

  public RequestGenerator getRequestGenerator() {
    requestGenerator.initialize(this);
    return requestGenerator;
  }

  public long getMaxUsersPerPoll() {
    return 100L;
  }

  public Duration getPollIntervalPerUser() {
    return Duration.ofMinutes(30);
  }
}
