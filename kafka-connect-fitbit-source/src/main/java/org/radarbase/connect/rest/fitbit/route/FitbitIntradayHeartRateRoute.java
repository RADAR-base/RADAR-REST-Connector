package org.radarbase.connect.rest.fitbit.route;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import io.confluent.connect.avro.AvroData;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.radarbase.connect.rest.fitbit.converter.FitbitIntradayHeartRateAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;

public class FitbitIntradayHeartRateRoute extends FitbitPollingRoute {
  private static final String ROUTE_NAME = "heart_rate";
  private final FitbitIntradayHeartRateAvroConverter converter;

  public FitbitIntradayHeartRateRoute(FitbitRequestGenerator generator,
      FitbitUserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, ROUTE_NAME);
    this.converter = new FitbitIntradayHeartRateAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/heart/date/%s/1d/1sec/time/%s/%s.json?timezone=UTC";
  }

  protected Stream<FitbitRestRequest> createRequests(FitbitUser user) {
    return startDateGenerator(getOffset(user).plus(ONE_SECOND).truncatedTo(SECONDS))
        .map(dateRange -> newRequest(user, dateRange.from().toInstant(), dateRange.to().toInstant(),
            user.getFitbitUserId(), DATE_FORMAT.format(dateRange.from()),
            ISO_LOCAL_TIME.format(dateRange.from()),
            ISO_LOCAL_TIME.format(dateRange.to().truncatedTo(SECONDS))));
  }

  @Override
  public FitbitIntradayHeartRateAvroConverter converter() {
    return converter;
  }
}
