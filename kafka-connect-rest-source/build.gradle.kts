description = "Kafka connector for generic REST API sources"

dependencies {
    api("com.squareup.okhttp3:okhttp:${Versions.okhttp}")

    // included in runtime
    compileOnly("org.apache.kafka:connect-api:${Versions.kafka}")
    compileOnly("org.slf4j:slf4j-api:${Versions.slf4j}")

    testImplementation("com.github.tomakehurst:wiremock:${Versions.wiremock}")
    testImplementation("org.apache.kafka:connect-api:${Versions.kafka}")
}
