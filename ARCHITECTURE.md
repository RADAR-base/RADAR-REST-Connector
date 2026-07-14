# Architecture

This document describes how RADAR-REST-Connector is put together, so that future contributors
(human or agent) can orient themselves quickly and add new device/API integrations (e.g. Huawei
Health Kit) consistently with the existing patterns.

## What this repo is

A multi-module Gradle project providing Kafka Connect **source connectors** that poll third-party
REST APIs (wearable vendor APIs) on behalf of RADAR-base study participants and publish the
resulting data as Avro records on Kafka topics. It currently ships two concrete connectors —
**Fitbit** and **Oura** — built on top of a shared, generic REST-polling framework.

```
RADAR-REST-Connector/
├── kafka-connect-rest-source/     # Generic Kafka Connect REST-source framework (Java)
├── kafka-connect-fitbit-source/   # Fitbit connector (Java), oldest/original implementation
├── oura-library/                  # Oura domain logic: routes, converters, requests (Kotlin, no Kafka Connect deps)
├── kafka-connect-oura-source/     # Oura Kafka Connect glue (Java+Kotlin), wraps oura-library
├── docker/                        # Docker Compose config templates, launch/ensure scripts, log4j
├── scripts/REDCAP-FITBIT-AUTH-AUTO/  # Standalone Python helper for REDCap-driven Fitbit auth
└── docker-compose.yml             # Full local Kafka stack + both connectors, for manual testing
```

Root Gradle config (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`) uses
the `org.radarbase.radar-kotlin` / `radar-root-project` plugins (from `radar-commons`) for shared
build conventions (Kotlin/Java toolchain, Sentry, versioning). All dependency versions are
centralized in `gradle/libs.versions.toml` (a Gradle version catalog) — add new deps there, not
inline in module build files.

Avro **schemas are not defined in this repo**. They come from the external `radar-schemas-commons`
artifact (`org.radarbase:radar-schemas-commons`, versioned in the catalog), generated from the
[RADAR-Schemas](https://github.com/RADAR-base/RADAR-Schemas) repository. Adding a new data type
therefore requires a schema to exist there first (e.g. `org.radarcns.connector.oura.OuraDailyActivity`),
before this repo can build an Avro record for it.

## Two architectural generations

The repo contains two different design generations. Understand both before adding Huawei, and
prefer the **Oura pattern** for new integrations — it is the more recent, more testable design.

### 1. Generic `kafka-connect-rest-source` framework (used by Fitbit)

Located at `kafka-connect-rest-source/src/main/java/org/radarbase/connect/rest/`. Defines a small
set of interfaces meant to be generic across arbitrary REST APIs:

- `AbstractRestSourceConnector` — Kafka Connect `SourceConnector` base class. Loads
  `RestSourceConnectorConfig` from properties and hands out `RestSourceTask` as the task class.
- `RestSourceConnectorConfig` — `AbstractConfig` wrapper exposing the generic
  `rest.source.*` properties (base URL, poll interval, topic selector class, payload converter
  class, request generator class — all pluggable via `ConfigDef.Type.CLASS`).
- `RequestGenerator` (`request/`) — produces a `Stream` of `RestRequest`s to issue and knows when
  the next request is due (`getTimeOfNextRequest()`), driven by the Kafka Connect offset storage
  (`setOffsetStorageReader`).
- `RequestRoute` / `PollingRequestRoute` (`request/`) — one "route" = one logical polling
  endpoint/data type. Routes own their own per-user polling cadence, backoff, and offset state, and
  are notified of `requestSucceeded` / `requestEmpty` / `requestFailed`.
- `PayloadToSourceRecordConverter` (`converter/`) — turns a raw HTTP response body into one or more
  Kafka Connect `SourceRecord`s.
- `RestSourceTask` — the actual Kafka Connect `SourceTask`. Its `poll()` loop: sleep until the next
  request is due, iterate `requestGenerator.requests()`, execute the first request that yields
  records, return them.

This module is intentionally protocol-agnostic; it has no notion of "Fitbit" or OAuth. It's a
reasonable place to fix or extend genuinely generic REST-polling behavior (e.g. topic selection,
generic retry semantics), but new device integrations do **not** need to hook into it directly —
see the Oura pattern below.

### 2. Fitbit connector (`kafka-connect-fitbit-source`) — first concrete integration

Built directly on the generic framework above, entirely in Java:

- `FitbitSourceConnector extends AbstractRestSourceConnector` — schedules a periodic
  (`application.loop.interval.ms`) user-repository refresh; if the user set changes, requests task
  reconfiguration (`context.requestTaskReconfiguration()`). Divides users across `tasks.max` tasks
  by hashing `user.getVersionedId()`.
- `FitbitRequestGenerator extends RequestGeneratorRouter` — builds the list of enabled
  `RequestRoute`s (one per Fitbit data type: sleep, activity log, resting heart rate, and — if
  `fitbit.api.intraday=true` — steps, heart rate, HRV, breathing rate, skin temperature, calories,
  SpO2) and an OkHttp client per user with a `TokenAuthenticator` (auto-refreshes on HTTP 401).
- `route/Fitbit*Route` — one class per data type, extending `FitbitPollingRoute`, which implements
  a fairly elaborate polling algorithm: don't poll more than once per `pollInterval`; walk history
  back to `HISTORICAL_TIME_DAYS`; avoid re-reading the last `LOOKBACK_TIME` to tolerate
  late-arriving data from other devices; back off per-user on HTTP 429 (`TOP_OF_HOUR` or
  `ROLLING_WINDOW` cooldown strategy) and after `fitbit.request.max.forbidden` consecutive HTTP 403s.
- `converter/Fitbit*AvroConverter` — one class per data type, converts JSON to the corresponding
  Avro record from `radar-schemas-commons`.
- `user/UserRepository` (interface) + implementations:
  - `YamlUserRepository` — reads per-user YAML files from a directory (`docker/fitbit-user.yml.template`
    shows the format: id, projectId, userId, sourceId, startDate/endDate, externalUserId, OAuth2
    access/refresh tokens).
  - `ServiceUserRepository` (Kotlin) — talks to an external "rest-source-authorizer" webservice
    (typically fronted by ManagementPortal) for user lists and token refresh/storage, using
    OAuth2 client-credentials auth.
  - `firebase/FirebaseUserRepository` / `CovidCollabFirebaseUserRepository` — legacy
    Firestore-backed repository for a specific historical deployment.
- `request/TokenAuthenticator` — OkHttp `Authenticator` that refreshes the access token via the
  `UserRepository` on a 401 and retries the request.

### 3. Oura connector (`oura-library` + `kafka-connect-oura-source`) — newer pattern

This is the template to follow for a **new** vendor integration such as Huawei. It splits cleanly
into:

- **`oura-library`** (pure Kotlin, no Kafka Connect / OkHttp-Authenticator coupling to Connect
  internals) — all vendor-specific domain logic, independently unit-testable and in principle
  reusable outside Kafka Connect:
  - `user/User`, `user/UserRepository` — user model and repository interface (`get`, `stream`,
    `getAccessToken`); note `refreshAccessToken` lives on the connector-side implementation, not
    here, in current Oura code (asymmetry vs. Fitbit — be aware when implementing).
  - `route/Route` (interface) + `route/OuraRoute` (abstract base) + `route/Oura*Route` (one per
    data type: daily activity, readiness, sleep, SpO2, heart rate, personal info, sessions,
    workouts, tags, ring configuration, stress, VO2 max, resilience, cardiovascular age, enhanced
    tags, rest-mode periods, sleep-time recommendations, etc.) — each route knows its API sub-path
    and builds one or more `RestRequest`s covering a `[start, end)` window, chunked by
    `maxIntervalPerRequest`.
  - `route/OuraRouteFactory` — central list of all routes (used mainly by tests / defaults; the
    connector module actually builds its own filtered list based on config flags — see below).
  - `converter/OuraDataConverter` (→ `RecordConverter`) + `converter/Oura*Converter` — one per data
    type, parses the JSON response into `TopicData(topic, key, value, offset)` where `value` is a
    generated Avro `SpecificRecord` from `radar-schemas-commons` (e.g. `OuraDailyActivity`). This
    is the direct analogue of Fitbit's `Fitbit*AvroConverter`, but returns plain data objects
    instead of Kafka Connect `SourceRecord`s directly — the Connect-specific wrapping happens in
    the connector module.
  - `request/OuraRequestGenerator` — the polling brain: for each `(route, user)` pair, computes the
    offset to resume from (via `OuraOffsetManager`), decides whether to use a large "historical"
    chunk (`HISTORICAL_QUERY_RANGE` = 1 year, once `timeSinceStart > HISTORICAL_DATA_THRESHOLD` = 1
    year) or normal recent-data chunking, and interprets HTTP responses
    (`handleResponse`/`requestSuccessful`/`requestFailed`) into typed `OuraResult`/`OuraError`
    sealed hierarchies with per-(route,user) backoff bookkeeping (`routeNextRequest` map) for 429 /
    403 / 401 / 400 / 422 / 404 / other.
  - `request/OuraOffsetManager` (interface) — abstraction for reading/writing per-(route,user)
    offsets; the Kafka Connect implementation is `KafkaOffsetManager` in the connector module.
  - `offset/Offset`, `offset/Offsets` — plain offset value types.
- **`kafka-connect-oura-source`** (Java, some Kotlin) — the thin Kafka Connect glue:
  - `OuraSourceConnector` — same role as `FitbitSourceConnector`: periodic user refresh,
    reconfiguration on user-set change, hash-based task partitioning.
  - `OuraSourceTask` — Kafka Connect `SourceTask`. Builds the enabled `Route` list from config
    flags, constructs `OuraRequestGenerator`, and in `poll()` round-robins across routes
    (`getRotatedRoutes()`, so one slow/rate-limited route doesn't starve the others), executes one
    HTTP request via a shared `OkHttpClient`, and converts the resulting `TopicData` list into
    Kafka Connect `SourceRecord`s using `AvroData` (Confluent's Kotlin/Avro↔Connect-schema bridge).
  - `offset/KafkaOffsetManager` — `OuraOffsetManager` backed by Kafka Connect's
    `OffsetStorageReader`.
  - `user/OuraUserRepository` (abstract) / `OuraServiceUserRepository` — the concrete
    "rest-source-authorizer" HTTP client, built on **Ktor** (not OkHttp) with `radar-commons`'s
    `CachedSet`/`CachedValue`/`clientCredentials` helpers for user list caching and per-user OAuth2
    token caching/refresh. This is the modern replacement for Fitbit's
    `ServiceUserRepository`/`TokenAuthenticator` combo, and is the pattern to copy for Huawei.
  - `OuraRestSourceConnectorConfig` — `ConfigDef` with one `oura.<type>.enabled` boolean and topic
    name per data type, plus `oura.user.repository.*` connection settings.

**Key structural difference from Fitbit**: Oura does *not* extend the generic
`kafka-connect-rest-source` interfaces (`RequestRoute`, `PollingRequestRoute`,
`PayloadToSourceRecordConverter`) at all — `OuraSourceTask` implements Kafka Connect's `SourceTask`
directly and drives `oura-library`'s own `Route`/`RequestGenerator`/`RecordConverter` abstractions.
This was a deliberate move to (a) get domain logic under unit test without spinning up Kafka
Connect, and (b) avoid the generic framework's assumptions (e.g. its polling-interval math) that
didn't fit Oura's simpler historical/recent chunking model.

## Runtime data flow (both connectors, conceptually)

```mermaid
sequenceDiagram
  participant connector as SourceConnector
  participant task as SourceTask
  participant userRepo as User Repository (rest-source-authorizer)
  participant api as Vendor API (Fitbit/Oura/…)
  participant kafka as Kafka

  connector ->> userRepo: Poll for users/config changes (periodic)
  connector ->> connector: Partition users across tasks.max tasks
  loop poll()
    task ->> task: Determine next due (route, user) request
    task ->> userRepo: Get/refresh OAuth2 access token
    task ->> api: GET data for date range
    api -->> task: JSON response
    task ->> task: Convert JSON -> Avro SourceRecord(s)
    task ->> kafka: Return records (Connect framework produces them)
    task ->> task: Update in-memory + Connect offset state
  end
```

User authentication/authorization data (OAuth2 tokens, study/user/source IDs, start/end dates) is
**not** stored in this repo. In production it's served by a "rest-source-authorizer" webservice
(part of RADAR-base, typically backed by ManagementPortal); for local/manual testing, Fitbit also
supports flat YAML files under `docker/users/` (`YamlUserRepository`).

## Configuration model

Every connector exposes its settings as a Kafka Connect `ConfigDef` (`org.apache.kafka.common.config`),
loaded from a Java `.properties` file (see `docker/source-fitbit.properties.template` and
`docker/source-oura.properties.template`) referenced by `connector.class`, `name`, `tasks.max`,
plus vendor-specific keys, e.g.:

- `<vendor>.api.client` / `<vendor>.api.secret` — OAuth2 app credentials.
- `<vendor>.user.repository.class` — pluggable `UserRepository` implementation.
- `<vendor>.user.repository.url` / `.client.id` / `.client.secret` / `.oauth2.token.url` —
  rest-source-authorizer connection details.
- `<vendor>.<data-type>.topic` / `.enabled` — per-data-type Kafka topic name and on/off switch, so
  studies can disable data types they don't need.

The full current list for Fitbit is documented in `README.md`; Oura's config lives in
`OuraRestSourceConnectorConfig` (no README table yet — check the class directly).

## Docker / deployment

Each connector module has its own multi-stage `Dockerfile` (Gradle build stage → base image
`confluentinc/cp-kafka-connect-base`), publishing built jars plus third-party deps into
`$CONNECT_PLUGIN_PATH/<module-name>/`. `docker/launch` and `docker/ensure` are modified Confluent
entrypoint scripts (env-var → properties translation, Kafka-readiness wait). `docker-compose.yml`
spins up a full local Zookeeper+Kafka+SchemaRegistry+REST-proxy cluster plus both connectors for
manual end-to-end testing (`docker-compose up -d --build`, inspect with
`kafka-avro-console-consumer`). Sentry error monitoring is wired in via `radarKotlin { sentryEnabled = true }`
and configured purely through `SENTRY_DSN`/`SENTRY_*` env vars — see README "Sentry monitoring".

## Testing

- `kafka-connect-rest-source/src/test`, `kafka-connect-fitbit-source/src/test`,
  `kafka-connect-oura-source/src/test` currently only contain config-parsing tests
  (`*ConnectorConfigTest`) plus one task test — test coverage of the actual polling/conversion
  logic is thin. `wiremock` and `mockito` are on the version catalog for HTTP-level testing but not
  yet exercised much; `oura-library`'s pure-Kotlin design makes it the easiest place to add real
  unit tests for new routes/converters without Kafka Connect scaffolding.
- CI (`.github/workflows/main.yml`) runs `./gradlew assemble` and `./gradlew check` on every push/PR
  to `master`/`dev`, then builds (and on `push`, publishes) multi-arch Docker images per connector
  module via a matrix job. `release.yml` does the same on GitHub Release publish, additionally
  uploading built jars as release assets, tagged `vX.Y.Z` from `gradle.properties`/version catalog.

## Adding a new vendor integration (e.g. Huawei)

Follow the **Oura pattern**, not the Fitbit one:

1. New Gradle module `huawei-library` (pure Kotlin, mirrors `oura-library`): `user/`, `route/`,
   `converter/`, `request/`, `offset/` packages. No Kafka Connect or OkHttp-Connect-specific types
   here — keep it independently testable.
2. New Gradle module `kafka-connect-huawei-source` (mirrors `kafka-connect-oura-source`):
   `HuaweiSourceConnector`, `HuaweiSourceTask`, `HuaweiRestSourceConnectorConfig`,
   `offset/KafkaOffsetManager`, `user/HuaweiServiceUserRepository` (Ktor-based
   rest-source-authorizer client, copy `OuraServiceUserRepository`'s structure), plus a
   `Dockerfile`.
3. Register both modules in `settings.gradle.kts`; add any new dependency versions to
   `gradle/libs.versions.toml` first.
4. Confirm (or add) the required Avro schemas in the external RADAR-Schemas project and bump the
   `radarSchemas` version in the catalog once published — this repo cannot invent schemas locally.
5. One `Route`/`Converter` pair per Huawei data type you plan to support, each independently
   togglable via a `huawei.<type>.enabled` config flag, matching the Oura/Fitbit convention.
6. Add `docker/source-huawei.properties.template`, a `docker-compose.yml` service entry, and a
   README config table, following the Fitbit/Oura sections as templates.
7. Add the new Docker image to the `IMAGES` matrix in both `.github/workflows/main.yml` and
   `release.yml`.
