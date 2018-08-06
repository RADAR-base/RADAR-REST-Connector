package org.radarbase.connect.rest.fitbit;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.errors.ConnectException;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.config.ValidClass;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.kafka.common.config.ConfigDef.NO_DEFAULT_VALUE;

public class FitbitRestSourceConnectorConfig extends RestSourceConnectorConfig {
  public static final String FITBIT_USERS_CONFIG = "fitbit.users";
  private static final String FITBIT_USERS_DOC =
      "Fitbit users with syntax fitbitUserName:refreshToken:projectId:userName:startDate:endDate"
          + "separated by commas.";
  private static final String FITBIT_USERS_DISPLAY = "Fitbit users";

  public static final String FITBIT_API_CLIENT_CONFIG = "fitbit.api.client";
  private static final String FITBIT_API_CLIENT_DOC =
      "Client ID for the Fitbit API";
  private static final String FITBIT_API_CLIENT_DISPLAY = "Fitbit API client ID";

  public static final String FITBIT_API_SECRET_CONFIG = "fitbit.api.secret";
  private static final String FITBIT_API_SECRET_DOC = "Secret for the Fitbit API client set in fitbit.api.client.";
  private static final String FITBIT_API_SECRET_DISPLAY = "Fitbit API client secret";

  public static final String FITBIT_USER_REPOSITORY_CONFIG = "fitbit.user.repository.class";
  private static final String FITBIT_USER_REPOSITORY_DOC = "Class for managing users and authentication.";
  private static final String FITBIT_USER_REPOSITORY_DISPLAY = "User repository class";

  public static final String FITBIT_USER_REPOSITORY_FILE_CONFIG = "fitbit.user.repository.file";
  private static final String FITBIT_USER_REPOSITORY_FILE_DOC = "File containing Fitbit users users and authentication. Only used if a file-based user repository is configured.";
  private static final String FITBIT_USER_REPOSITORY_FILE_DISPLAY = "User repository file";

  private static final String FITBIT_INTRADAY_STEPS_TOPIC_CONFIG = "fitbit.intraday.steps.topic";

  private final FitbitUserRepository fitbitUserRepository;
  private String fitbitSleepStageTopic;

  @SuppressWarnings("unchecked")
  public FitbitRestSourceConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
    super(config, parsedConfig);

    try {
      fitbitUserRepository = ((Class<? extends FitbitUserRepository>)
          getClass(FITBIT_USER_REPOSITORY_CONFIG)).getDeclaredConstructor().newInstance();
    } catch (IllegalAccessException | InstantiationException
        | InvocationTargetException | NoSuchMethodException e) {
      throw new ConnectException("Invalid class for: " + SOURCE_PAYLOAD_CONVERTER_CONFIG, e);
    }
  }

  public FitbitRestSourceConnectorConfig(Map<String, String> parsedConfig) {
    this(FitbitRestSourceConnectorConfig.conf(), parsedConfig);
  }

  public static ConfigDef conf() {
    int orderInGroup = 0;
    String group = "Fitbit";
    return RestSourceConnectorConfig.conf()

        .define(FITBIT_USERS_CONFIG,
            ConfigDef.Type.LIST,
            NO_DEFAULT_VALUE,
            new ValidFitbitUsers(),
            ConfigDef.Importance.HIGH,
            FITBIT_USERS_DOC,
            group,
            ++orderInGroup,
            ConfigDef.Width.SHORT,
            FITBIT_USERS_DISPLAY)

        .define(FITBIT_API_CLIENT_CONFIG,
            ConfigDef.Type.STRING,
            NO_DEFAULT_VALUE,
            new ConfigDef.NonEmptyString(),
            ConfigDef.Importance.HIGH,
            FITBIT_API_CLIENT_DOC,
            group,
            ++orderInGroup,
            ConfigDef.Width.SHORT,
            FITBIT_API_CLIENT_DISPLAY)

        .define(FITBIT_API_SECRET_CONFIG,
            ConfigDef.Type.STRING,
            NO_DEFAULT_VALUE,
            new ConfigDef.NonEmptyString(),
            ConfigDef.Importance.HIGH,
            FITBIT_API_SECRET_DOC,
            group,
            ++orderInGroup,
            ConfigDef.Width.SHORT,
            FITBIT_API_SECRET_DISPLAY)

        .define(FITBIT_USER_REPOSITORY_CONFIG,
            ConfigDef.Type.CLASS,
            YamlFitbitUserRepository.class,
            ValidClass.isSubclassOf(FitbitUserRepository.class),
            ConfigDef.Importance.MEDIUM,
            FITBIT_USER_REPOSITORY_DOC,
            group,
            ++orderInGroup,
            ConfigDef.Width.SHORT,
            FITBIT_USER_REPOSITORY_DISPLAY)

        .define(FITBIT_USER_REPOSITORY_FILE_CONFIG,
            ConfigDef.Type.STRING,
            "/etc/kafka-connect/fitbit-users.yml",
            ConfigDef.Importance.LOW,
            FITBIT_USER_REPOSITORY_FILE_DOC,
            group,
            ++orderInGroup,
            ConfigDef.Width.SHORT,
            FITBIT_USER_REPOSITORY_FILE_DISPLAY)
        ;
  }

  public List<String> getRawFitbitUsers() {
    return getList(FITBIT_USERS_CONFIG);
  }

  public List<FitbitUser> getFitbitUsers() {
    return getRawFitbitUsers().stream()
        .map(FitbitUser::parse)
        .collect(Collectors.toList());
  }

  public String getFitbitClient() {
    return getString(FITBIT_API_CLIENT_CONFIG);
  }

  public String getFitbitClientSecret() {
    return getString(FITBIT_API_SECRET_CONFIG);
  }

  public FitbitUserRepository getFitbitUserRepository() {
    return fitbitUserRepository;
  }

  public String getFitbitIntradayStepsTopic() {
    return getString(FITBIT_INTRADAY_STEPS_TOPIC_CONFIG);
  }

  public String getFitbitSleepStageTopic() {
    return fitbitSleepStageTopic;
  }
}
