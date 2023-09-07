description = "Kafka connector for Oura API source"

dependencies {
    api(project(":kafka-connect-rest-source"))
    api(project(":oura-library"))
    api("io.confluent:kafka-connect-avro-converter:${Versions.confluent}")
    api("org.radarbase:radar-schemas-commons:${Versions.radarSchemas}")
    implementation("org.radarbase:oauth-client-util:${Versions.managementPortal}")

    implementation(platform("com.fasterxml.jackson:jackson-bom:${Versions.jackson}"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.google.firebase:firebase-admin:${Versions.firebaseAdmin}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.21")

    // Included in connector runtime
    compileOnly("org.apache.kafka:connect-api:${Versions.kafka}")
    compileOnly(platform("com.fasterxml.jackson:jackson-bom:${Versions.jackson}"))
    compileOnly("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.apache.kafka:connect-api:${Versions.kafka}")
}
