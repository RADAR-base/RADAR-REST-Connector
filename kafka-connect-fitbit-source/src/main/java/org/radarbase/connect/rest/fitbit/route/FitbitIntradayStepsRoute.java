package org.radarbase.connect.rest.fitbit.route;

import okhttp3.Request;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;
import org.radarbase.connect.rest.fitbit.converter.FitbitIntradayStepsAvroConverter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class FitbitIntradayStepsRoute extends FitbitPollingRoute {
  private static final String ROUTE_NAME = "intraday_steps";
  private final FitbitIntradayStepsAvroConverter converter;
  private String urlFormat;

  public FitbitIntradayStepsRoute(FitbitRequestGenerator generator, FitbitUserRepository userRepository) {
    super(generator, userRepository, ROUTE_NAME);
    this.converter = new FitbitIntradayStepsAvroConverter();
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    super.initialize(config);
    this.urlFormat = config.getUrl() + "/1/user/%s/activities/steps/date/%s/1m.json";
  }

  protected Request makeRequest(FitbitUser user) {
    ZonedDateTime date = Instant.ofEpochMilli(this.getOffset(user)).atZone(ZoneOffset.UTC);

    return new Request.Builder()
        .header("Authorization", "Bearer " + user.getAccessToken())
        .header("x-li-format", "json")
        .url(String.format(this.urlFormat, user.getFitbitUserId(), DATE_FORMAT.format(date)))
        .build();
  }

  @Override
  public FitbitIntradayStepsAvroConverter converter() {
    return converter;
  }
}
