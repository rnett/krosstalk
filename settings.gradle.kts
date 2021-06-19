import com.rnett.future.testing.kotlinFutureTesting

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
plugins {
    id("com.github.rnett.kotlin-future-testing") version "0.0.11-SNAPSHOT"
}

kotlinFutureTesting {
    generateGithubWorkflows(branch = "main") {
        bothCommands(
            "./gradlew check",
            "(cd tests && ./gradlew check)",
            suffix = "test"
        )
        bothCommands(
            "./gradlew check",
            "(cd tests && ./gradlew check)",
            suffix = "compile"
        )
    }
}

rootProject.name = "krosstalk-parent"

include(
    "core",
    "core:krosstalk",
    "core:krosstalk-base",
    "core:krosstalk-client",
    "core:krosstalk-server",
    "compiler:krosstalk-compiler-plugin",
    "compiler:krosstalk-compiler-plugin-native",
    "compiler:krosstalk-gradle-plugin"
)

include(
    "plugins:ktor-server:krosstalk-ktor-server",
    "plugins:ktor-server:krosstalk-ktor-server-auth",
    "plugins:ktor-server:krosstalk-ktor-server-auth-jwt"
)

include(
    "plugins",
    "plugins:ktor-client:krosstalk-ktor-client",
    "plugins:ktor-client:krosstalk-ktor-client-auth"
)

include(
    "plugins:krosstalk-kotlinx-serialization"
)

