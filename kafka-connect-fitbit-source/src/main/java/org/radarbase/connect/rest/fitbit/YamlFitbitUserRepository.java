package org.radarbase.connect.rest.fitbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.config.FitbitUserConfig;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class YamlFitbitUserRepository implements FitbitUserRepository {
  private static final YAMLFactory yamlFactory = new YAMLFactory();
  private static final ObjectMapper yamlMapper = new ObjectMapper(yamlFactory);
  static {
    yamlMapper.registerModule(new JavaTimeModule());
  }
  private static final ObjectReader userReader = yamlMapper.readerFor(FitbitUserConfig.class);
  private static final ObjectWriter userWriter = yamlMapper.writerFor(FitbitUserConfig.class);

  private Path path;
  private Set<String> configuredUsers;
  private FitbitUserConfig users;

  private FitbitUserConfig readUsers() throws IOException {
    if (users == null) {
      users = userReader.readValue(path.toFile());
    }
    return users;
  }

  @Override
  public FitbitUser get(String combinedName) throws IOException {
    return readUsers().get(combinedName);
  }

  @Override
  public Stream<FitbitUser> stream() throws IOException {
    Stream<FitbitUser> users = readUsers().stream();
    if (!configuredUsers.isEmpty()) {
      users = users.filter(u -> configuredUsers.contains(u.getKey()));
    }
    return users;
  }

  @Override
  public void update(FitbitUser user) throws IOException {
    Path temp = Files.createTempFile("users", "yml");
    try {
      try (OutputStream out = Files.newOutputStream(temp);
           BufferedOutputStream bufOut = new BufferedOutputStream(out)) {
        userWriter.writeValue(bufOut, users);
      }
      Files.move(temp, path, REPLACE_EXISTING);
    } catch (IOException ex) {
      Files.deleteIfExists(temp);
    }
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    path = config.getUserFilePath();
    configuredUsers = new HashSet<>(fitbitConfig.getFitbitUsers());
  }
}
