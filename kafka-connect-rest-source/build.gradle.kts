description = "Kafka connector for generic REST API sources"

dependencies {
    api(libs.okhttp)

    // included in runtime
    compileOnly(libs.kafka.connect.api)
    compileOnly(libs.slf4j.api)

    testImplementation(libs.mockito.core)
    testImplementation(libs.wiremock)

    testImplementation(libs.kafka.connect.api)

    // Application monitoring
    // These dependencies are not used by the REST connector, but copied into the Docker image (Dockerfile)
    runtimeOnly(libs.sentry.log4j) {
        // Exclude log4j with security vulnerability (safe version is provided by docker image).
        exclude(group = "log4j", module = "log4j")
    }
    runtimeOnly(libs.sentry.opentelemetry.agent)
}
