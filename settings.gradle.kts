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
    id("com.github.rnett.kotlin-future-testing") version "0.0.12-SNAPSHOT"
}

kotlinFutureTesting {
    generateGithubWorkflows(branch = "main", runners = listOf("ubuntu-latest", "macos-latest", "windows-latest"), force = true) {
        commonCommands("chmod +x  tests/gradlew")
        commonStep("""
          - name: Install CURL Linux
            if: runner.os == 'Linux'
            run: |
              sudo apt-get install curl libcurl4 libcurl4-openssl-dev -y
        """.trimIndent())

        bothCommands("./gradlew assemble", "(cd tests && ./gradlew assemble)", suffix = "compile")
        bothCommands("./gradlew check", "(cd tests && ./gradlew check)", suffix = "test")
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

