rootProject.name = "kafka-connect-rest-source"
include(":kafka-connect-fitbit-source")
include(":kafka-connect-rest-source")
include(":kafka-connect-oura-source")
include(":oura-library")
include(":google-health-library")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
