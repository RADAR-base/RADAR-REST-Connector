package org.radarbase.connect.rest.fitbit.route;

import io.confluent.connect.avro.AvroData;
import okhttp3.Request;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.converter.FitbitSleepAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class FitbitSleepRoute extends FitbitPollingRoute {
  private static final String ROUTE_NAME = "sleep";
  private final FitbitSleepAvroConverter converter;
  private String urlFormat;

  public FitbitSleepRoute(FitbitRequestGenerator generator, FitbitUserRepository userRepository,
      AvroData avroData) {
    super(generator, userRepository, ROUTE_NAME);
    converter = new FitbitSleepAvroConverter(avroData);
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
  protected FitbitRestRequest makeRequest(FitbitUser user) {
    ZonedDateTime startDate = this.getOffset(user)
        .atZone(ZoneOffset.UTC)
        .truncatedTo(ChronoUnit.DAYS);

    ZonedDateTime endDate = Instant.now().minus(LOOKBACK_TIME)
        .atZone(ZoneOffset.UTC)
        .truncatedTo(ChronoUnit.DAYS);

    // encode
    Request request = new Request.Builder()
        .header("Authorization", "Bearer " + user.getAccessToken())
        .header("x-li-format", "json")
        .url(String.format(this.urlFormat, user.getFitbitUserId(),
            DATE_FORMAT.format(startDate), DATE_FORMAT.format(endDate)))
        .build();

    return newRequest(request, user, startDate.toInstant(),
        endDate.toInstant().plus(Duration.ofDays(1)));
  }

  @Override
  public FitbitSleepAvroConverter converter() {
    return converter;
  }
}
