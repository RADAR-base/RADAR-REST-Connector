package org.radarbase.connect.rest.fitbit.route;

import io.confluent.connect.avro.AvroData;
import java.time.Duration;
import java.time.Instant;
import org.radarbase.connect.rest.fitbit.converter.FitbitTimeZoneAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;

public class FitbitTimeZoneRoute extends FitbitPollingRoute {
  private final FitbitTimeZoneAvroConverter converter;

  public FitbitTimeZoneRoute(FitbitRequestGenerator generator,
      FitbitUserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "timezone");
    this.converter = new FitbitTimeZoneAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/profile.json";
  }

  protected FitbitRestRequest makeRequest(FitbitUser user) {
    Instant now = Instant.now();
    return newRequest(user, now, now, user.getFitbitUserId());
  }

  @Override
  public FitbitTimeZoneAvroConverter converter() {
    return converter;
  }

  @Override
  protected Duration getLookbackTime() {
    return Duration.ofHours(1);
  }
}
