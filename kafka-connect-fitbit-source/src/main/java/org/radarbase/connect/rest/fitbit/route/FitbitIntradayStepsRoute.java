package org.radarbase.connect.rest.fitbit.route;

import io.confluent.connect.avro.AvroData;
import okhttp3.Request;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.converter.FitbitIntradayStepsAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class FitbitIntradayStepsRoute extends FitbitPollingRoute {
  private static final String ROUTE_NAME = "intraday_steps";
  private final FitbitIntradayStepsAvroConverter converter;
  private String urlFormat;

  public FitbitIntradayStepsRoute(FitbitRequestGenerator generator,
      FitbitUserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, ROUTE_NAME);
    this.converter = new FitbitIntradayStepsAvroConverter(avroData);
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    super.initialize(config);
    this.urlFormat = config.getUrl() + "/1/user/%s/activities/steps/date/%s/1min/time/%s/%s.json";
  }

  protected FitbitRestRequest makeRequest(FitbitUser user) {
    ZonedDateTime startDate = this.getOffset(user)
        .atZone(ZoneOffset.UTC)
        .plus(Duration.ofMinutes(1))
        .truncatedTo(ChronoUnit.MINUTES);

    ZonedDateTime endDate = startDate.withHour(23).withMinute(59);

    Request request = new Request.Builder()
        .header("Authorization", "Bearer " + user.getAccessToken())
        .header("x-li-format", "json")
        .url(String.format(this.urlFormat,
            user.getFitbitUserId(), DATE_FORMAT.format(startDate),
            TIME_FORMAT.format(startDate), TIME_FORMAT.format(endDate)))
        .build();

    return newRequest(request, user, startDate.toInstant(), endDate.toInstant());
  }

  @Override
  public FitbitIntradayStepsAvroConverter converter() {
    return converter;
  }
}
