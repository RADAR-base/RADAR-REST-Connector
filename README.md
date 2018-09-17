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
your Fitbit App client ID and client secret.

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
