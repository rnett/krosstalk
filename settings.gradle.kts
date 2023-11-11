pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }

}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

includeBuild("gradle/plugins")

rootProject.name = "krosstalk"

include(
    "core",
    "client",
    "server",
    "processor"
)