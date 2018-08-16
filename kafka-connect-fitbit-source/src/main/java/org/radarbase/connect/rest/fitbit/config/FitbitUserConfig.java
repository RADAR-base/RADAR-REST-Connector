package org.radarbase.connect.rest.fitbit.config;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FitbitUserConfig {
  @JsonIgnore
  private Map<String, LocalFitbitUser> users;

  public LocalFitbitUser get(String id) {
    return users.get(id);
  }

  @JsonSetter("users")
  public void setUsers(Collection<LocalFitbitUser> users) {
    this.users = users.stream()
        .collect(Collectors.toConcurrentMap(LocalFitbitUser::getId, Function.identity()));
  }

  @JsonGetter("users")
  public Collection<LocalFitbitUser> getUsers() {
    return users.values();
  }

  public Stream<LocalFitbitUser> stream() {
    return this.users.values().stream();
  }
}
