description = "Kafka connector for Huawei Health Kit API source"

dependencies {

    /* The entries in the block below are added here to force the version of
     * transitive dependencies and mitigate reported vulnerabilities
     */
    implementation(libs.netty.handler.proxy)
    implementation(libs.netty.handler)

    api(project(":huawei-library"))
    api(libs.kafka.connect.avro.converter)
    api(libs.radar.schemas.commons.huawei)
    implementation(libs.radar.commons.kotlin)

    api(libs.okhttp)
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.kotlin.stdlib)

    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.jackson.module.kotlin)

    // Included in connector runtime
    compileOnly(libs.kafka.connect.api)
    compileOnly(platform(libs.jackson.bom))
    compileOnly(libs.jackson.databind)

    testImplementation(libs.kafka.connect.api)
    testImplementation(libs.wiremock)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlin.test)
}
