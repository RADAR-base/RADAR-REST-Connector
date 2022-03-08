package org.radarbase.connect.rest.fitbit.route;

import io.confluent.connect.avro.AvroData;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.convert.fitbit.FitbitDataConverter;
import org.radarbase.convert.fitbit.FitbitIntradayCaloriesDataConverter;

import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.MINUTES;

public class FitbitIntradayCaloriesRoute extends FitbitPollingRoute {
  public FitbitIntradayCaloriesRoute(
      FitbitRequestGenerator generator, UserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "intraday_calories", avroData);
  }

  @Override
  protected FitbitDataConverter createConverter(FitbitRestSourceConnectorConfig config) {
    return new FitbitIntradayCaloriesDataConverter(config.getFitbitIntradayCaloriesTopic());
  }

  @Override
  protected Stream<FitbitRestRequest> createRequests(User user) {
    return startDateGenerator(this.getOffset(user).plus(ONE_MINUTE).truncatedTo(MINUTES))
        .map(
            dateRange ->
                newRequest(
                    user,
                    dateRange,
                    user.getExternalUserId(),
                    DATE_FORMAT.format(dateRange.getStart()),
                    TIME_FORMAT.format(dateRange.getStart()),
                    TIME_FORMAT.format(dateRange.getEnd())));
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/calories/date/%s/1d/1min/time/%s/%s.json?timezone=UTC";
  }
}
