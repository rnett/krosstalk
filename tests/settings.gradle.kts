pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }
    }

}

rootProject.name = "tests"

includeBuild("..")
include(
    "fullstack-test",
    "client-test",
    "compose-test",
    "microservices-test",
    "microservices-test:ping",
    "microservices-test:pong"
)

