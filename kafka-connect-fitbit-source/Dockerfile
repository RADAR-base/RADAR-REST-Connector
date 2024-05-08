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

FROM --platform=$BUILDPLATFORM gradle:8.4-jdk17 as builder

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

FROM confluentinc/cp-kafka-connect-base:7.5.0

MAINTAINER Joris Borgdorff <joris@thehyve.nl>

LABEL description="Kafka REST API Source connector"

ENV CONNECT_PLUGIN_PATH="/usr/share/java/kafka-connect/plugins" \
  WAIT_FOR_KAFKA="1"

# To isolate the classpath from the plugin path as recommended
COPY --from=builder /code/kafka-connect-rest-source/build/third-party/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-rest-source/
COPY --from=builder /code/kafka-connect-fitbit-source/build/third-party/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-fitbit-source/

COPY --from=builder /code/kafka-connect-rest-source/build/libs/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-rest-source/
COPY --from=builder /code/kafka-connect-rest-source/build/libs/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-fitbit-source/
COPY --from=builder /code/kafka-connect-fitbit-source/build/libs/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-fitbit-source/

# Load topics validator
COPY  --chown=appuser:appuser ./docker/kafka-wait /usr/bin/kafka-wait

# Load modified launcher
COPY  --chown=appuser:appuser ./docker/launch /etc/confluent/docker/launch
