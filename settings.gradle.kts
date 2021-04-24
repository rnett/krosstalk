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

rootProject.name = "krosstalk-parent"

include(
    "core:krosstalk", "core:krosstalk-core",
    "core:krosstalk-client", "core:krosstalk-server",
    "compiler:krosstalk-compiler-plugin", "compiler:krosstalk-gradle-plugin"
)

include(
    "plugins:ktor-server:krosstalk-ktor-server",
    "plugins:ktor-server:krosstalk-ktor-server-auth",
    "plugins:ktor-server:krosstalk-ktor-server-auth-jwt"
)

include(
    "plugins:ktor-client:krosstalk-ktor-client",
    "plugins:ktor-client:krosstalk-ktor-client-auth"
)

include(
    "plugins:kotlinx-serialization:krosstalk-kotlinx-serialization-cbor",
    "plugins:kotlinx-serialization:krosstalk-kotlinx-serialization-json"
)

