package org.radarbase.connect.rest.fitbit.route;

import static java.time.temporal.ChronoUnit.MINUTES;

import io.confluent.connect.avro.AvroData;
import java.time.temporal.TemporalAmount;
import java.util.stream.Stream;
import org.radarbase.connect.rest.fitbit.converter.FitbitIntradayStepsAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;

public class FitbitIntradayStepsRoute extends FitbitPollingRoute {
  private static final TemporalAmount ONE_MINUTE = MINUTES.getDuration();

  private final FitbitIntradayStepsAvroConverter converter;

  public FitbitIntradayStepsRoute(FitbitRequestGenerator generator,
      FitbitUserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "intraday_steps");
    this.converter = new FitbitIntradayStepsAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/steps/date/%s/1d/1min/time/%s/%s.json?timezone=UTC";
  }

  protected Stream<FitbitRestRequest> createRequests(FitbitUser user) {
    return startDateGenerator(this.getOffset(user).plus(ONE_MINUTE).truncatedTo(MINUTES))
        .map(dateRange -> newRequest(user, dateRange,
            user.getFitbitUserId(), DATE_FORMAT.format(dateRange.start()),
            TIME_FORMAT.format(dateRange.start()), TIME_FORMAT.format(dateRange.end())));
  }

  @Override
  public FitbitIntradayStepsAvroConverter converter() {
    return converter;
  }
}
