plugins {
    // TODO Remove this when new release of radar-commons is available and used in this project.
    // This version has Sentry support built in for radar-kotlin plugin.
    id("io.sentry.jvm.gradle") version "4.11.0"
}

description = "Kafka connector for generic REST API sources"

dependencies {
    api("com.squareup.okhttp3:okhttp:${Versions.okhttp}")

    // included in runtime
    compileOnly("org.apache.kafka:connect-api:${Versions.kafka}")
    compileOnly("org.slf4j:slf4j-api:${Versions.slf4j}")

    testImplementation("org.mockito:mockito-core:${Versions.mockito}")
    testImplementation("com.github.tomakehurst:wiremock:${Versions.wiremock}")

    testImplementation("org.apache.kafka:connect-api:${Versions.kafka}")
}
