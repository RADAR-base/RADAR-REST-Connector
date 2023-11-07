rootProject.name = "kafka-connect-rest-source"
include(":kafka-connect-fitbit-source")
include(":kafka-connect-rest-source")
include(":oura-library")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
