import org.radarbase.gradle.plugin.radarKotlin

plugins {
    alias(libs.plugins.radar.root.project)
    alias(libs.plugins.radar.dependency.management)
    alias(libs.plugins.radar.kotlin) apply false
}

repositories {
    mavenCentral()
}

description = "Kafka connector for REST API sources"

radarRootProject {
    projectVersion.set(libs.versions.project)
    gradleVersion.set(libs.versions.gradle)
}

subprojects {
    apply(plugin = "org.radarbase.radar-kotlin")

    // --- Vulnerability fixes start ---
    dependencies {
        plugins.withType<JavaPlugin> {
            constraints {
                add("implementation", rootProject.libs.jackson.bom) {
                    because("Force safe version of Jackson across all modules")
                }
                add("implementation", rootProject.libs.commons.lang3) {
                    because("Force safe version of commons-lang3 across all modules")
                }
            }
        }
    }
    // --- Vulnerability fixes end ---

    radarKotlin {
        log4j2Version.set(rootProject.libs.versions.log4j2)
        sentryEnabled.set(true)
        openTelemetryAgentEnabled.set(false)
    }
}
