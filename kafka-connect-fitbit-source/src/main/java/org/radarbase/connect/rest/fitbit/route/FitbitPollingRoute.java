/*
 * Copyright 2018 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.radarbase.connect.rest.fitbit.route;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.MIN_INSTANT;
import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.TIMESTAMP_OFFSET_KEY;
import static org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter.nearFuture;
import static org.radarbase.connect.rest.request.PollingRequestRoute.max;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.radarbase.connect.rest.RestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.FitbitRestSourceConnectorConfig;
import org.radarbase.connect.rest.fitbit.request.FitbitRequestGenerator;
import org.radarbase.connect.rest.fitbit.request.FitbitRestRequest;
import org.radarbase.connect.rest.fitbit.user.User;
import org.radarbase.connect.rest.fitbit.user.UserNotAuthorizedException;
import org.radarbase.connect.rest.fitbit.user.UserRepository;
import org.radarbase.connect.rest.fitbit.util.DateRange;
import org.radarbase.connect.rest.request.PollingRequestRoute;
import org.radarbase.connect.rest.request.RestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Route for regular polling.
 *
 * <p>The algorithm uses the following polling times:
 * 1. do not try polling until getLastPoll() + getPollInterval()
 * 2. if that has passed, determine for each user when to poll again. Per user:
 *    1. if a successful call was made that returned data, take the last successful offset and after
 *       getLookbackTime() has passed, poll again.
 *    2. if a successful call was made that did not return data, take the last query interval
 *       and start cycling up from the last successful record, starting no further than
 *       HISTORICAL_TIME
 *
 * <p>Conditions that should be met:
 * 1. Do not poll more frequently than once every getPollInterval().
 * 2. On first addition of a user, poll its entire history
 * 3. If the history of a user has been scanned, do not look back further than
 *    {@code HISTORICAL_TIME}. This ensures fewer operations under normal operations, where Fitbit
 *    data is fairly frequently updated.
 * 4. If there was data for a certain date time in an API, earlier date times are not polled. This
 *    prevents duplicate data.
 * 5. From after the latest known date time, the history of the user is regularly inspected for new
 *    records.
 * 6, All of the recent history is simultaneously inspected to prevent reading only later data in
 *    a single batch that is added to the API.
 * 6. Do not try to read any records for the last {@code getLookbackTime()}. This lookback time ensures that
 *    when new records are added by another device within the {@code getLookbackTime()} period, they are also
 *    added.
 * 7. When a too many records exception occurs, do not poll for given user for
 *    {@code TOO_MANY_REQUESTS_COOLDOWN}.
 */
public abstract class FitbitPollingRoute implements PollingRequestRoute {
  protected static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  protected static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
  protected static final Duration LOOKBACK_TIME = Duration.ofDays(1); // 1 day
  protected static final long HISTORICAL_TIME_DAYS = 14L;
  protected static final Duration ONE_DAY = DAYS.getDuration();
  protected static final Duration THIRTY_DAYS = Duration.ofDays(30);
  protected static final Duration ONE_NANO = NANOS.getDuration();
  protected static final TemporalAmount ONE_SECOND = SECONDS.getDuration();
  protected static final TemporalAmount ONE_MINUTE = MINUTES.getDuration();

  private static final Logger logger = LoggerFactory.getLogger(FitbitSleepRoute.class);

  /** Committed offsets. */
  private Map<String, Instant> offsets;

  private final Map<String, Map<String, Object>> partitions;
  private final Map<String, Instant> lastPollPerUser;
  private final FitbitRequestGenerator generator;
  private final UserRepository userRepository;
  private final String routeName;
  private Duration pollInterval;
  private Instant lastPoll;
  private String baseUrl;
  private Duration pollIntervalPerUser;
  private final Set<User> tooManyRequestsForUser;
  private Duration tooManyRequestsCooldown;

  public FitbitPollingRoute(
      FitbitRequestGenerator generator,
      UserRepository userRepository,
      String routeName) {
    this.generator = generator;
    this.userRepository = userRepository;
    this.offsets = new HashMap<>();
    this.partitions = new HashMap<>(generator.getPartitions(routeName));
    this.routeName = routeName;
    this.lastPoll = MIN_INSTANT;
    this.lastPollPerUser = new HashMap<>();
    this.tooManyRequestsForUser = ConcurrentHashMap.newKeySet();
  }

  @Override
  public void initialize(RestSourceConnectorConfig config) {
    FitbitRestSourceConnectorConfig fitbitConfig = (FitbitRestSourceConnectorConfig) config;
    this.pollInterval = fitbitConfig.getPollInterval();
    this.baseUrl = fitbitConfig.getUrl();
    this.pollIntervalPerUser = fitbitConfig.getPollIntervalPerUser();
    this.tooManyRequestsCooldown = fitbitConfig.getTooManyRequestsCooldownInterval()
        .minus(getPollIntervalPerUser());
    this.converter().initialize(fitbitConfig);
  }

  @Override
  public void requestSucceeded(RestRequest request, SourceRecord record) {
    lastPollPerUser.put(((FitbitRestRequest) request).getUser().getId(), lastPoll);
    String userKey = ((FitbitRestRequest) request).getUser().getVersionedId();
    Instant offset = Instant.ofEpochMilli((Long) record.sourceOffset().get(TIMESTAMP_OFFSET_KEY));
    offsets.put(userKey, offset);
  }

  @Override
  public void requestEmpty(RestRequest request) {
    lastPollPerUser.put(((FitbitRestRequest) request).getUser().getId(), lastPoll);
    FitbitRestRequest fitbitRequest = (FitbitRestRequest) request;
    Instant endOffset = fitbitRequest.getDateRange().end().toInstant();
    if (DAYS.between(endOffset, lastPoll) >= HISTORICAL_TIME_DAYS) {
      String key = fitbitRequest.getUser().getVersionedId();
      offsets.put(key, endOffset);
    }
  }

  @Override
  public void requestFailed(RestRequest request, Response response) {
    if (response != null && response.code() == 429) {
      User user = ((FitbitRestRequest)request).getUser();
      tooManyRequestsForUser.add(user);
      String cooldownString = response.header("Retry-After");
      Duration cooldown = getTooManyRequestsCooldown();
      if (cooldownString != null) {
        try {
          cooldown = Duration.ofSeconds(Long.parseLong(cooldownString));
        } catch (NumberFormatException ex) {
          cooldown = getTooManyRequestsCooldown();
        }
      }
      Instant backOff = lastPoll.plus(cooldown);
      lastPollPerUser.put(user.getId(), backOff);
      logger.info("Too many requests for user {}. Backing off until {}",
          user, backOff.plus(getPollIntervalPerUser()));
    } else {
      logger.warn("Failed to make request {}", request);
    }
  }

  /**
   * Actually construct requests, based on the current offset
   * @param user Fitbit user
   * @return request to make
   */
  protected abstract Stream<FitbitRestRequest> createRequests(User user);

  @Override
  public Stream<FitbitRestRequest> requests() {
    tooManyRequestsForUser.clear();
    lastPoll = Instant.now();
    try {
      return userRepository.stream()
          .map(u -> new AbstractMap.SimpleImmutableEntry<>(u, nextPoll(u)))
          .filter(u -> lastPoll.isAfter(u.getValue()))
          .sorted(Map.Entry.comparingByValue())
          .flatMap(u -> this.createRequests(u.getKey()))
          .filter(Objects::nonNull);
    } catch (IOException e) {
      logger.warn("Cannot read users");
      return Stream.empty();
    }
  }


  /** Get the time that this route should be polled again. */
  @Override
  public Instant getTimeOfNextRequest() {
    return nextPolls()
            .min(Comparator.naturalOrder())
            .orElse(nearFuture());
  }

  private Map<String, Object> getPartition(User user) {
    return partitions.computeIfAbsent(user.getVersionedId(),
        k -> generator.getPartition(routeName, user));
  }

  /**
   * Create a FitbitRestRequest for given arguments.
   * @param user Fitbit user
   * @param dateRange dates that may be queried in the request
   * @param urlFormatArgs format arguments to {@link #getUrlFormat(String)}.
   * @return request or {@code null} if the authorization cannot be arranged.
   */
  protected FitbitRestRequest newRequest(User user, DateRange dateRange,
      Object... urlFormatArgs) {
    Request.Builder builder = new Request.Builder()
        .url(String.format(getUrlFormat(baseUrl), urlFormatArgs));
    try {
      Request request = builder
          .header("Authorization", "Bearer " + userRepository.getAccessToken(user))
          .build();
      return new FitbitRestRequest(this, request, user, getPartition(user),
          generator.getClient(user), dateRange,
          req -> !tooManyRequestsForUser.contains(((FitbitRestRequest)req).getUser()));
    } catch (UserNotAuthorizedException | IOException ex) {
      logger.warn("User {} does not have a configured access token: {}. Skipping.",
          user, ex.toString());
      return null;
    }
  }

  @Override
  public void setOffsetStorageReader(OffsetStorageReader offsetStorageReader) {
    if (offsetStorageReader != null) {
      offsets = offsetStorageReader.offsets(partitions.values()).entrySet().stream()
              .filter(e -> e.getValue() != null && e.getValue().containsKey(TIMESTAMP_OFFSET_KEY))
              .collect(Collectors.toMap(
                  e -> (String) e.getKey().get("user"),
                  e -> Instant.ofEpochMilli((Long) e.getValue().get(TIMESTAMP_OFFSET_KEY))));
    } else {
      logger.warn("Offset storage reader is null, will resume from an empty state.");
    }
  }

  @Override
  public Duration getPollInterval() {
    return pollInterval;
  }

  @Override
  public Stream<Instant> nextPolls() {
    try {
      return userRepository.stream()
          .map(this::nextPoll);
    } catch (IOException e) {
      logger.warn("Failed to read users for polling interval: {}", e.toString());
      return Stream.of(getLastPoll().plus(getPollInterval()));
    }
  }

  public Instant getLastPoll() {
    return lastPoll;
  }

  protected Instant getOffset(User user) {
    return offsets.getOrDefault(user.getVersionedId(), user.getStartDate().minus(ONE_NANO));
  }

  /**
   * URL String format. The format arguments should be provided to
   * {@link #newRequest(User, DateRange, Object...)}
   */
  protected abstract String getUrlFormat(String baseUrl);

  /**
   * Get the poll interval for a single user on a single route.
   */
  protected Duration getPollIntervalPerUser() {
    return pollIntervalPerUser;
  }

  protected Duration getTooManyRequestsCooldown() {
    return tooManyRequestsCooldown;
  }

  /**
   * Time that should not be polled to avoid duplicate data.
   */
  protected Duration getLookbackTime() {
    return LOOKBACK_TIME;
  }

  /**
   * Next time that given user should be polled.
   */
  protected Instant nextPoll(User user) {
    Instant offset = getOffset(user);
    if (offset.isAfter(user.getEndDate().minus(getEndDateThreshold()))) {
      return nearFuture();
    } else {
      Instant nextPoll = lastPollPerUser.getOrDefault(user.getId(), MIN_INSTANT)
          .plus(getPollIntervalPerUser());
      return max(offset.plus(getLookbackTime()), nextPoll);
    }
  }

  private TemporalAmount getEndDateThreshold() {
    return Duration.ofHours(1);
  }

  /**
   * Generate one date per day (or specified rangeInterval), using UTC time zone. The first date will have the time from the
   * given startDate. Following time stamps will start at 00:00. This will not up to the date of
   * {@link #getLookbackTime()} (exclusive).
   */
  Stream<DateRange> startDateGenerator(Instant startDate) {
    Instant lookBack = lastPoll.minus(getLookbackTime());

    ZonedDateTime dateTime = startDate.atZone(UTC);
    ZonedDateTime lookBackDate = lookBack.atZone(UTC);
    ZonedDateTime lookBackDateStart = lookBackDate.truncatedTo(DAYS);

    // last date to poll is equal to the last polled date
    if (lookBackDateStart.equals(dateTime.truncatedTo(DAYS))) {
      if (lookBack.isAfter(startDate)) {
        return Stream.of(new DateRange(dateTime, lookBackDate));
      } else {
        return Stream.empty();
      }
    } else {
      Duration rangeInterval = getDateRangeInterval();

      Stream<DateRange> elements = Stream
          .iterate(dateTime, t -> t.plus(rangeInterval).truncatedTo(DAYS))
          .takeWhile(u -> u.isBefore(lookBackDateStart))
          .map(s -> new DateRange(s, s.plus(rangeInterval).truncatedTo(DAYS).minus(ONE_NANO)));

      // we're polling at exactly 00:00, should not poll the last date
      if (lookBackDateStart.equals(lookBackDate)) {
        return elements;
      } else {
        // we're polling startDate - night, x days, lastDay - lookBackDate
        return Stream.concat(elements,
            Stream.of(new DateRange(lookBackDateStart, lookBackDate)));
      }
    }
  }

  Duration getDateRangeInterval() {
    return ONE_DAY;
  }
}
