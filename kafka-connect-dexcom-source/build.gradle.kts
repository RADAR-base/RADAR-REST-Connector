description = "Kafka connector for Dexcom API source"

dependencies {

    /* The entries in the block below are added here to force the version of
     * transitive dependencies and mitigate reported vulnerabilities
     */
    implementation(libs.netty.handler.proxy)
    implementation(libs.netty.handler)

    api(project(":dexcom-library"))
    api(libs.kafka.connect.avro.converter)
    api(libs.radar.schemas.commons)
    implementation(libs.radar.commons.kotlin)

    api(libs.okhttp)
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.firebase.admin)
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
}