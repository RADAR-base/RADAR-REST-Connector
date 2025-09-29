import org.radarbase.gradle.plugin.radarKotlin

plugins {
    id("org.radarbase.radar-root-project") version Versions.radarCommons
    id("org.radarbase.radar-dependency-management") version Versions.radarCommons
    id("org.radarbase.radar-kotlin") version Versions.radarCommons apply false
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    mavenCentral()
}

description = "Kafka connector for REST API sources"

radarRootProject {
    projectVersion.set(Versions.project)
    gradleVersion.set(Versions.wrapper)
}

subprojects {
    apply(plugin = "org.radarbase.radar-kotlin")

    configurations.all {
        resolutionStrategy {
            /* The entries in the block below are added here to force the version of
             * transitive dependencies and mitigate reported vulnerabilities */
            force(
                "org.apache.commons:commons-lang3:3.18.0",
            )
        }
    }

    radarKotlin {
        javaVersion.set(Versions.java)
        kotlinVersion.set(Versions.kotlin)
        slf4jVersion.set(Versions.slf4j)
        log4j2Version.set(Versions.log4j2)
        junitVersion.set(Versions.junit)
    }
}
