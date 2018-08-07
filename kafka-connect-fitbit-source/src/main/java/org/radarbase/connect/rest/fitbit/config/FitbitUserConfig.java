package org.radarbase.connect.rest.fitbit.config;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSetter;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FitbitUserConfig {
  @JsonIgnore
  private Map<String, FitbitUser> users;

  public FitbitUser get(String combinedName) {
    return users.get(combinedName);
  }

  @JsonSetter("users")
  public void setUsers(Collection<FitbitUser> users) {
    this.users = users.stream()
        .collect(Collectors.toConcurrentMap(FitbitUser::getKey, Function.identity()));
  }

  @JsonGetter("users")
  public Collection<FitbitUser> getUsers() {
    return users.values();
  }

  public Stream<FitbitUser> stream() {
    return this.users.values().stream();
  }
}
