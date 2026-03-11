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

    configurations.all {
        resolutionStrategy {
            /* The entries in the block below are added here to force the version of
             * transitive dependencies and mitigate reported vulnerabilities */
            force(
                "org.apache.commons:commons-lang3:${rootProject.libs.versions.commonsLang3.get()}",
            )
        }
    }

    radarKotlin {
        javaVersion.set(rootProject.libs.versions.java.get().toInt())
        kotlinVersion.set(rootProject.libs.versions.kotlin)
        slf4jVersion.set(rootProject.libs.versions.slf4j)
        log4j2Version.set(rootProject.libs.versions.log4j2)
        junitVersion.set(rootProject.libs.versions.junit)
    }
}
