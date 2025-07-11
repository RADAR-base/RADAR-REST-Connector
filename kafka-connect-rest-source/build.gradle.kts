description = "Kafka connector for generic REST API sources"

dependencies {
    api("com.squareup.okhttp3:okhttp:${Versions.okhttp}")

    // included in runtime
    compileOnly("org.apache.kafka:connect-api:${Versions.kafka}")
    compileOnly("org.slf4j:slf4j-api:${Versions.slf4j}")

    testImplementation("org.mockito:mockito-core:${Versions.mockito}")
    testImplementation("com.github.tomakehurst:wiremock:${Versions.wiremock}")

    testImplementation("org.apache.kafka:connect-api:${Versions.kafka}")

    // Application monitoring
    // These dependencies are not used by the REST connector, but copied into the Docker image (Dockerfile)
    runtimeOnly("io.sentry:sentry-log4j:${Versions.sentryLog4j}") {
        // Exclude log4j with security vulnerability (safe version is provided by docker image).
        exclude(group = "log4j", module = "log4j")
    }
    runtimeOnly("io.sentry:sentry-opentelemetry-agent:${Versions.sentryOpenTelemetryAgent}")
}
