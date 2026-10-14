import groovy.json.JsonOutput

plugins {
    distribution
}

description = "Kafka connector for Oura API source"

dependencies {

    /* The entries in the block below are added here to force the version of
     * transitive dependencies and mitigate reported vulnerabilities
     */
    implementation(libs.netty.handler.proxy)
    implementation(libs.netty.handler)

    api(project(":oura-library"))
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

// Self-contained Kafka Connect plugin zip, for example for a distributed Kafka Connect cluster or
// Amazon MSK Connect. It follows the Confluent Hub component archive layout, so that
// `confluent-hub install` also accepts it:
// https://docs.confluent.io/platform/current/connect/confluent-hub/component-archive.html
val connectorManifest = tasks.register("connectorManifest") {
    val manifestFile = layout.buildDirectory.file("connector-manifest/manifest.json")
    val githubUrl = "https://github.com/RADAR-base/RADAR-REST-Connector"
    val manifest = provider {
        JsonOutput.prettyPrint(
            JsonOutput.toJson(
                mapOf(
                    "name" to project.name,
                    "version" to project.version.toString(),
                    "title" to "RADAR-base Oura Source Connector",
                    "description" to project.description,
                    "owner" to mapOf(
                        "username" to "radarbase",
                        "name" to "RADAR-base",
                        "type" to "organization",
                        "url" to "https://radar-base.org",
                    ),
                    "component_types" to listOf("source"),
                    "source_url" to githubUrl,
                    "documentation_url" to "$githubUrl#readme",
                    "license" to listOf(
                        mapOf(
                            "name" to "Apache License 2.0",
                            "url" to "https://www.apache.org/licenses/LICENSE-2.0",
                        ),
                    ),
                ),
            ),
        )
    }
    inputs.property("manifest", manifest)
    outputs.file(manifestFile)
    doLast {
        manifestFile.get().asFile.writeText(manifest.get())
    }
}

distributions {
    main {
        contents {
            from(connectorManifest)
            into("lib") {
                from(tasks.jar)
                from(configurations.runtimeClasspath)
            }
            into("doc") {
                from(rootProject.file("LICENSE"), rootProject.file("README.md"))
            }
        }
    }
}

// Only the zip is released.
tasks.distTar {
    enabled = false
}
