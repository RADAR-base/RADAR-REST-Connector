rootProject.name = "kafka-connect-rest-source"
include(":kafka-connect-fitbit-source")
include(":kafka-connect-rest-source")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
