import org.radarbase.gradle.plugin.radarKotlin
import org.radarbase.gradle.plugin.radarPublishing

plugins {
    alias(libs.plugins.radar.root.project)
    alias(libs.plugins.radar.dependency.management)
    alias(libs.plugins.radar.kotlin) apply false
    alias(libs.plugins.radar.publishing) apply false
}

repositories {
    mavenCentral()
}

description = "Kafka connector for REST API sources"

radarRootProject {
    projectVersion.set(libs.versions.project)
    gradleVersion.set(libs.versions.gradle)
}

val githubRepoName = "RADAR-base/RADAR-REST-Connector"
val githubProjectUrl = "https://github.com/$githubRepoName"

val publishedSubprojects = setOf("oura-library", "google-health-library")

subprojects {
    apply(plugin = "org.radarbase.radar-kotlin")

    if (name in publishedSubprojects) {
        apply(plugin = "org.radarbase.radar-publishing")
        radarPublishing {
            githubUrl.set(githubProjectUrl)
            developers {
                developer {
                    id.set("yatharthranjan")
                    name.set("Yatharth Ranjan")
                    email.set("yatharth.ranjan@kcl.ac.uk")
                    organization.set("King's College London")
                    id.set("mpgxvii")
                    name.set("Pauline Conde")
                    email.set("mpgxvii@gmail.com")
                    organization.set("King's College London")
                    id.set("this-Aditya")
                    name.set("Aditya Mishra")
                    email.set("aditya.mishra@kcl.ac.uk")
                    organization.set("King's College London")
                }
            }
        }
    }

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
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            // Substitute the old group/module with drop-in replacement
            substitute(module("org.lz4:lz4-java"))
                .using(module(rootProject.libs.lz4.get().toString()))
                .because("Force safe version of LZ4 across all modules")
        }
    }
    // --- Vulnerability fixes end ---

    radarKotlin {
        log4j2Version.set(rootProject.libs.versions.log4j2)
        sentryEnabled.set(true)
        openTelemetryAgentEnabled.set(false)
    }
}
