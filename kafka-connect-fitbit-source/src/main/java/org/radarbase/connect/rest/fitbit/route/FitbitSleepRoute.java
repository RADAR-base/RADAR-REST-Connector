package org.radarbase.connect.rest.fitbit.route;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;

import io.confluent.connect.avro.AvroData;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.radarbase.connect.rest.fitbit.converter.FitbitSleepAvroConverter;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.FitbitUser;
import org.radarbase.connect.rest.fitbit.user.FitbitUserRepository;

public class FitbitSleepRoute extends FitbitPollingRoute {
  public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
      .withZone(UTC);
  private static final Duration SLEEP_POLL_INTERVAL = Duration.ofDays(1);

  private final FitbitSleepAvroConverter converter;

  public FitbitSleepRoute(FitbitRequestGenerator generator, FitbitUserRepository userRepository,
      AvroData avroData) {
    super(generator, userRepository, "sleep");
    converter = new FitbitSleepAvroConverter(avroData);
  }

  @Override
  protected String getUrlFormat(String baseUrl) {
    return baseUrl + "/1.2/user/%s/sleep/list.json?sort=asc&afterDate=%s&limit=100&offset=0";
  }

  /**
   * Actually construct a request, based on the current offset
   * @param user Fitbit user
   * @return request to make
   */
  protected Stream<FitbitRestRequest> createRequests(FitbitUser user) {
    ZonedDateTime startDate = this.getOffset(user).plus(ONE_SECOND)
        .atZone(UTC)
        .truncatedTo(SECONDS);

    return Stream.of(newRequest(user, startDate.toInstant(), Instant.now(), user.getFitbitUserId(),
        DATE_TIME_FORMAT.format(startDate)));
  }


  @Override
  protected Duration getPollIntervalPerUser() {
    return SLEEP_POLL_INTERVAL;
  }

  @Override
  public FitbitSleepAvroConverter converter() {
    return converter;
  }
}
