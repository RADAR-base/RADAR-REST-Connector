# Kafka Connect REST Source and Fitbit Source

This project contains a Kafka Connect source connector for a general REST API, and one for
Fitbit in particular. The documentation of the Kafka Connect REST source still needs to be done.

## Fitbit source connector

### Installation

This repository relies on a recent version of docker and docker-compose as well as an installation
of Java 8 or later.

### Usage

First, [register a Fitbit App](https://dev.fitbit.com/apps) with Fitbit. It should be either a
server app, for multiple users, or a personal app for a single user. With the server app, you need
to [request access to intraday API data](https://dev.fitbit.com/build/reference/web-api/help/).

For every Fitbit user you want access to, copy `docker/fitbit-user.yml.template` to a file in
`docker/users/`. Get an access token and refresh token for the user using for example the
[Fitbit OAuth 2.0 tutorial page](https://dev.fitbit.com/apps/oauthinteractivetutorial).

For automatic configuration for multiple users, please take a look at `scripts/REDCAP-FITBIT-AUTH-AUTO/README.md`.

Copy `docker/source-fitbit.properties.template` to `docker/source-fitbit.properties` and enter
your Fitbit App client ID and client secret. The following tables shows the possible properties.

<table class="data-table"><tbody>
<tr>
<th>Name</th>
<th>Description</th>
<th>Type</th>
<th>Default</th>
<th>Valid Values</th>
<th>Importance</th>
</tr>
<tr>
<td>rest.source.poll.interval.ms</td></td><td>How often to poll the source URL.</td></td><td>long</td></td><td>60000</td></td><td></td></td><td>low</td></td></tr>
<tr>
<td>rest.source.base.url</td></td><td>Base URL for REST source connector.</td></td><td>string</td></td><td></td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>rest.source.destination.topics</td></td><td>The  list of destination topics for the REST source connector.</td></td><td>list</td></td><td>""</td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>rest.source.topic.selector</td></td><td>The topic selector class for REST source connector.</td></td><td>class</td></td><td>org.radarbase.connect.rest.selector.SimpleTopicSelector</td></td><td>Class extending org.radarbase.connect.rest.selector.TopicSelector</td></td><td>high</td></td></tr>
<tr>
<td>rest.source.payload.converter.class</td></td><td>Class to be used to convert messages from REST calls to SourceRecords</td></td><td>class</td></td><td>org.radarbase.connect.rest.converter.StringPayloadConverter</td></td><td>Class extending org.radarbase.connect.rest.converter.PayloadToSourceRecordConverter</td></td><td>low</td></td></tr>
<tr>
<td>rest.source.request.generator.class</td></td><td>Class to be used to generate REST requests</td></td><td>class</td></td><td>org.radarbase.connect.rest.single.SingleRequestGenerator</td></td><td>Class extending org.radarbase.connect.rest.request.RequestGenerator</td></td><td>low</td></td></tr>
<tr>
<td>fitbit.users</td></td><td>The user ID of Fitbit users to include in polling, separated by commas. Non existing user names will be ignored. If empty, all users in the user directory will be used.</td></td><td>list</td></td><td>""</td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>fitbit.api.client</td></td><td>Client ID for the Fitbit API</td></td><td>string</td></td><td></td></td><td>non-empty string</td></td><td>high</td></td></tr>
<tr>
<td>fitbit.api.secret</td></td><td>Secret for the Fitbit API client set in fitbit.api.client.</td></td><td>password</td></td><td></td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>fitbit.api.intraday</td></td><td>Set to true if the client has permissions to Fitbit Intraday API, false otherwise.</td></td><td>boolean</td></td><td>true</td></td><td></td></td><td>medium</td></td></tr>
<tr>
<td>fitbit.user.repository.classes</td></td><td>Class(es) for managing users and authentication. Comma separate for using multiple repositories</td></td><td>list</td></td><td>org.radarbase.connect.rest.fitbit.user.YamlUserRepository</td></td><td>Classes extending org.radarbase.connect.rest.fitbit.user.UserRepository</td></td><td>medium</td></td></tr>
<tr>
<td>fitbit.user.dir</td></td><td>Directory containing Fitbit user information and credentials. Only used if a file-based user repository is configured.</td></td><td>string</td></td><td>/var/lib/kafka-connect-fitbit-source/users</td></td><td></td></td><td>low</td></td></tr>
<tr>
<td>fitbit.user.repository.url</td></td><td>URL for webservice containing user credentials. Only used if a webservice-based user repository is configured.</td></td><td>string</td></td><td>""</td></td><td></td></td><td>low</td></td></tr>
<tr>
<td>fitbit.max.users.per.poll</td></td><td>Maximum number of users to query in a single poll operation. Decrease this if memory constrains are pressing.</td></td><td>int</td></td><td>100</td></td><td>[1,...]</td></td><td>low</td></td></tr>
<tr>
<td>fitbit.intraday.steps.topic</td></td><td>Topic for Fitbit intraday steps</td></td><td>string</td></td><td>connect_fitbit_intraday_steps</td></td><td>non-empty string without control characters</td></td><td>low</td></td></tr>
<tr>
<td>fitbit.intraday.heart.rate.topic</td></td><td>Topic for Fitbit intraday heart_rate</td></td><td>string</td></td><td>connect_fitbit_intraday_heart_rate</td></td><td>non-empty string without control characters</td></td><td>low</td></td></tr>
<tr>
<td>fitbit.sleep.stages.topic</td></td><td>Topic for Fitbit sleep stages</td></td><td>string</td></td><td>connect_fitbit_sleep_stages</td></td><td>non-empty string without control characters</td></td><td>low</td></td></tr>
<tr>
<td>fitbit.sleep.classic.topic</td></td><td>Topic for Fitbit sleep classic data</td></td><td>string</td></td><td>connect_fitbit_sleep_classic</td></td><td>non-empty string without control characters</td></td><td>low</td></td></tr>
<tr>
<td>fitbit.time.zone.topic</td></td><td>Topic for Fitbit profile time zone</td></td><td>string</td></td><td>connect_fitbit_time_zone</td></td><td>non-empty string without control characters</td></td><td>low</td></td></tr>
<tr>
<td>fitbit.activity.log.topic</td></td><td>Topic for Fitbit activity log.</td></td><td>string</td></td><td>connect_fitbit_activity_log</td></td><td>non-empty string without control characters</td></td><td>low</td></td></tr>
</tbody></table>

Now you can run a full Kafka stack using

```shell
docker-compose up -d --build
```

Inspect the progress with `docker-compose logs -f radar-fitbit-connector`. To inspect data
that is coming out of the requests, run

```shell
docker-compose exec schema-registry-1 kafka-avro-console-consumer \
  --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --from-beginning \
  --topic connect_fitbit_intraday_heart_rate
```

## Contributing

Code should be formatted using the [Google Java Code Style Guide](https://google.github.io/styleguide/javaguide.html).
If you want to contribute a feature or fix browse our [issues](https://github.com/RADAR-base/RADAR-REST-Connector/issues), and please make a pull request.
