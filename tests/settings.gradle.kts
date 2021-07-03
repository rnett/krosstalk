import com.rnett.future.testing.kotlinFutureTesting

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
plugins {
    id("com.github.rnett.kotlin-future-testing") version "0.0.12-SNAPSHOT"
}

kotlinFutureTesting {
    forceBootstrap()
}

rootProject.name = "tests"

includeBuild("..")
include(
    "fullstack-test",
    "client-test",
    "native-test",
//    "compose-test",
    "microservices-test",
    "microservices-test:ping",
    "microservices-test:pong"
)

