import org.radarbase.gradle.plugin.radarKotlin
import org.radarbase.gradle.plugin.radarPublishing

plugins {
    id("org.radarbase.radar-root-project") version Versions.radarCommons
    id("org.radarbase.radar-dependency-management") version Versions.radarCommons
    id("org.radarbase.radar-kotlin") version Versions.radarCommons apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
}

description = "Kafka connector for REST API sources"

radarRootProject {
    projectVersion.set(Versions.project)
}

subprojects {
    apply(plugin = "org.radarbase.radar-kotlin")

    radarKotlin {
        javaVersion.set(Versions.java)
        kotlinVersion.set(Versions.kotlin)
        slf4jVersion.set(Versions.slf4j)
        log4j2Version.set(Versions.log4j2)
        junitVersion.set(Versions.junit)
    }
}
