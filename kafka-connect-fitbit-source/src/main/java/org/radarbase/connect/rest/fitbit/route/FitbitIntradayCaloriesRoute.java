package org.radarbase.connect.rest.fitbit.route;

import static java.time.temporal.ChronoUnit.MINUTES;

import io.confluent.connect.avro.AvroData;
import java.util.stream.Stream;
import org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter;
import org.radarbase.connect.rest.fitbit.converter.FitbitIntradayCaloriesAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserRepository;

public class FitbitIntradayCaloriesRoute extends FitbitPollingRoute {

  private final FitbitIntradayCaloriesAvroConverter caloriesAvroConverter;

  public FitbitIntradayCaloriesRoute(
      FitbitRequestGenerator generator, UserRepository userRepository, AvroData avroData) {
    super(generator, userRepository, "intraday_calories");
    caloriesAvroConverter = new FitbitIntradayCaloriesAvroConverter(avroData);
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
                    DATE_FORMAT.format(dateRange.start()),
                    TIME_FORMAT.format(dateRange.start()),
                    TIME_FORMAT.format(dateRange.end())));
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1/user/%s/activities/calories/date/%s/1d/1min/time/%s/%s.json?timezone=UTC";
  }

  @Override
  public PayloadToSourceRecordConverter converter() {
    return caloriesAvroConverter;
  }
}
