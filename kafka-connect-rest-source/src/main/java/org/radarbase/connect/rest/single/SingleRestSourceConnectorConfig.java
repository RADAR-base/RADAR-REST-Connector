package org.radarbase.connect.rest.single;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.config.MethodRecommender;
import org.radarbase.connect.rest.config.MethodValidator;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.kafka.common.config.ConfigDef.NO_DEFAULT_VALUE;

public class SingleRestSourceConnectorConfig extends RestSourceConnectorConfig {
  public static final String SOURCE_METHOD_CONFIG = "rest.source.method";
  private static final String SOURCE_METHOD_DOC = "The HTTP method for REST source connector.";
  private static final String SOURCE_METHOD_DISPLAY = "Source method";
  private static final String SOURCE_METHOD_DEFAULT = "POST";

  public static final String SOURCE_PROPERTIES_LIST_CONFIG = "rest.source.properties";
  private static final String SOURCE_PROPERTIES_LIST_DOC =
      "The request properties (headers) for REST source connector.";
  private static final String SOURCE_PROPERTIES_LIST_DISPLAY = "Source properties";

  public static final String SOURCE_DATA_CONFIG = "rest.source.data";
  private static final String SOURCE_DATA_DOC = "The data for REST source connector.";
  private static final String SOURCE_DATA_DISPLAY = "Data for REST source connector.";
  private static final String SOURCE_DATA_DEFAULT = null;

  private final Map<String, String> requestProperties;

  @SuppressWarnings("unchecked")
  private SingleRestSourceConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
    super(config, parsedConfig);
    requestProperties = getPropertiesList().stream()
        .map(COLON_PATTERN::split)
        .collect(Collectors.toMap(a -> a[0], a -> a[1]));
  }

  public SingleRestSourceConnectorConfig(Map<String, String> parsedConfig) {
    this(SingleRestSourceConnectorConfig.conf(), parsedConfig);
  }

  public static ConfigDef conf() {
    String group = "Single REST source";
    ConfigDef superConf = RestSourceConnectorConfig.conf();
    int orderInGroup = superConf.names().size();
    return superConf
        .define(SOURCE_METHOD_CONFIG,
            Type.STRING,
            SOURCE_METHOD_DEFAULT,
            new MethodValidator(),
            Importance.HIGH,
            SOURCE_METHOD_DOC,
            group,
            ++orderInGroup,
            ConfigDef.Width.SHORT,
            SOURCE_METHOD_DISPLAY,
            new MethodRecommender())

        .define(SOURCE_PROPERTIES_LIST_CONFIG,
            Type.LIST,
            NO_DEFAULT_VALUE,
            Importance.HIGH,
            SOURCE_PROPERTIES_LIST_DOC,
            group,
            ++orderInGroup,
            ConfigDef.Width.SHORT,
            SOURCE_PROPERTIES_LIST_DISPLAY)

        .define(SOURCE_DATA_CONFIG,
            Type.STRING,
            SOURCE_DATA_DEFAULT,
            Importance.LOW,
            SOURCE_DATA_DOC,
            group,
            ++orderInGroup,
            ConfigDef.Width.SHORT,
            SOURCE_DATA_DISPLAY);
  }

  public List<String> getPropertiesList() {
    return this.getList(SOURCE_PROPERTIES_LIST_CONFIG);
  }

  public String getMethod() {
    return this.getString(SOURCE_METHOD_CONFIG);
  }

  public String getData() {
    return this.getString(SOURCE_DATA_CONFIG);
  }

  public Map<String, String> getRequestProperties() {
    return requestProperties;
  }

  public static void main(String[] args) {
    System.out.println(conf().toHtmlTable());
  }
}
