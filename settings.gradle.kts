rootProject.name = "kafka-connect-rest-source"
include(":kafka-connect-fitbit-source")
include(":kafka-connect-rest-source")
include(":oura-library")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://maven.pkg.github.com/radar-base/radar-commons") {
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                    ?: extra.properties["gpr.user"] as? String
                    ?: extra.properties["public.gpr.user"] as? String
                password = System.getenv("GITHUB_TOKEN")
                    ?: extra.properties["gpr.token"] as? String
                    ?: (extra.properties["public.gpr.token"] as? String)?.let {
                        java.util.Base64.getDecoder().decode(it).decodeToString()
                    }
            }
        }
    }
}
