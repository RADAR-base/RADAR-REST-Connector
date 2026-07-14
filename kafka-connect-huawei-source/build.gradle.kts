description = "Kafka connector for Huawei Health Kit API source"

repositories {
    // Prefer a locally-published snapshot (e.g. built by hand from the RADAR-Schemas
    // huawei_schemas branch via `./gradlew :radar-schemas-commons:publishToMavenLocal`) before
    // falling back to remote snapshot hosts.
    mavenLocal()

    // radar-schemas-commons huawei_schemas is only published as a snapshot; declare the
    // candidate snapshot hosts here so the build can resolve it regardless of which one the
    // RADAR-Schemas release pipeline currently targets.
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://maven.pkg.github.com/RADAR-base/RADAR-Schemas")
        credentials {
            username = project.findProperty("public.gpr.user") as String? ?: System.getenv("GPR_USER")
            password = project.findProperty("public.gpr.token") as String? ?: System.getenv("GPR_TOKEN")
        }
    }
}

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
}
