package org.radarbase.connect.rest.fitbit.route;

import okhttp3.Request;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.FitbitUser;
import org.radarbase.connect.rest.fitbit.FitbitUserRepository;
import org.radarbase.connect.rest.fitbit.converter.FitbitSleepAvroConverter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class FitbitSleepRoute extends FitbitPollingRoute {
  private static final String ROUTE_NAME = "sleep";
  private final FitbitSleepAvroConverter converter;
  private String urlFormat;

  public FitbitSleepRoute(FitbitRequestGenerator generator, FitbitUserRepository userRepository) {
    super(generator, userRepository, ROUTE_NAME);
    converter = new FitbitSleepAvroConverter();
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    super.initialize(config);
    this.urlFormat = config.getUrl() + "/1.2/user/%s/sleep/date/%s/%s.json";
  }

  /**
   * Actually construct a request, based on the current offset
   * @param user Fitbit user
   * @return request to make
   */
  protected Request makeRequest(FitbitUser user) {
    ZonedDateTime startDate = Instant.ofEpochMilli(this.getOffset(user))
        .atZone(ZoneOffset.UTC);
    ZonedDateTime endDate = Instant.ofEpochMilli(System.currentTimeMillis() - LOOKBACK_TIME)
        .atZone(ZoneOffset.UTC);

    // encode
    return new Request.Builder()
        .header("Authorization", "Bearer " + user.getAccessToken())
        .header("x-li-format", "json")
        .url(String.format(this.urlFormat, user.getFitbitUserId(),
            DATE_FORMAT.format(startDate), DATE_FORMAT.format(endDate)))
        .build();
  }

  @Override
  public FitbitSleepAvroConverter converter() {
    return converter;
  }
}
