package org.radarbase.connect.rest.fitbit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.config.ConfigException;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.config.FitbitUserConfig;
import org.radarbase.connect.rest.fitbit.util.SynchronizedFileAccess;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class YamlFitbitUserRepository implements FitbitUserRepository {
  private static final YAMLFactory YAML_FACTORY = new YAMLFactory();
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAML_FACTORY);
  static {
    YAML_MAPPER.registerModule(new JavaTimeModule());
  }

  private Set<String> configuredUsers;
  private SynchronizedFileAccess<FitbitUserConfig> users;

  @Override
  public FitbitUser get(String combinedName) {
    return users.get().get(combinedName);
  }

  @Override
  public Stream<FitbitUser> stream() {
    Stream<FitbitUser> users = this.users.get().stream();
    if (!configuredUsers.isEmpty()) {
      users = users.filter(u -> configuredUsers.contains(u.getKey()));
    }
    return users;
  }

  @Override
  public void update(FitbitUser user) throws IOException {
    this.users.store();
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    try {
      this.users = SynchronizedFileAccess.ofPath(config.getUserFilePath(), YAML_MAPPER, FitbitUserConfig.class);
    } catch (IOException ex) {
      throw new ConfigException("Failed to read user repository " + config.getUserFilePath(), ex);
    }
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    configuredUsers = new HashSet<>(fitbitConfig.getFitbitUsers());
  }
}
