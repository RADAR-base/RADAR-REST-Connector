# Copyright 2018 The Hyve
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM --platform=$BUILDPLATFORM gradle:8.9-jdk17 AS builder

RUN mkdir /code
WORKDIR /code

ENV GRADLE_USER_HOME=/code/.gradlecache \
  GRADLE_OPTS="-Dorg.gradle.vfs.watch=false -Djdk.lang.Process.launchMechanism=vfork"

COPY buildSrc /code/buildSrc
COPY ./build.gradle.kts ./settings.gradle.kts ./gradle.properties /code/
COPY kafka-connect-rest-source/build.gradle.kts /code/kafka-connect-rest-source/
COPY kafka-connect-fitbit-source/build.gradle.kts /code/kafka-connect-fitbit-source/

RUN gradle downloadDependencies copyDependencies

COPY ./kafka-connect-rest-source/src/ /code/kafka-connect-rest-source/src
COPY ./kafka-connect-fitbit-source/src/ /code/kafka-connect-fitbit-source/src

RUN gradle jar

FROM quay.io/strimzi/kafka:0.46.0-kafka-3.9.0

USER appuser

LABEL org.opencontainers.image.authors="pim@thehyve.nl"

LABEL description="Kafka REST API Source connector"

ENV CONNECT_PLUGIN_PATH=/opt/kafka/plugins

# To isolate the classpath from the plugin path as recommended
COPY --from=builder /code/kafka-connect-rest-source/build/third-party/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-rest-source/
COPY --from=builder /code/kafka-connect-fitbit-source/build/third-party/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-fitbit-source/

COPY --from=builder /code/kafka-connect-rest-source/build/libs/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-rest-source/
COPY --from=builder /code/kafka-connect-rest-source/build/libs/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-fitbit-source/
COPY --from=builder /code/kafka-connect-fitbit-source/build/libs/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-fitbit-source/

# Overwrite the log4j configuration to include Sentry monitoring.
# TODO uncomment after new log4j2 properties file is added.
# COPY ./docker/log4j2.properties /opt/kafka/custom-config/log4j.properties
# Copy Sentry monitoring jars.
COPY --from=builder /code/kafka-connect-fitbit-source/build/third-party/sentry-* /opt/kafka/libs/

USER 1001

COPY --chown=1001:1001 ./docker/ensure /opt/kafka/ensure
COPY --chown=1001:1001 ./docker/kafka_connect_run.sh /opt/kafka/kafka_connect_run.sh
RUN chmod +x /opt/kafka/ensure /opt/kafka/kafka_connect_run.sh
