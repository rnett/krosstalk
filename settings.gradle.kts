pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }
    }

}

rootProject.name = "krosstalk-parent"

include(
    "core:krosstalk", "core:krosstalk-core",
    "core:krosstalk-client", "core:krosstalk-server",
    "plugins:krosstalk-compiler-plugin", "plugins:krosstalk-gradle-plugin",
    "ktor:krosstalk-ktor-server", "ktor:krosstalk-ktor-client"
)

