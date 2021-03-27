pluginManagement {
    repositories {
        jcenter()
        mavenCentral()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        mavenLocal()
    }

}
//enableFeaturePreview("GRADLE_METADATA")
rootProject.name = "krosstalk-parent"

include(
    "core:krosstalk", "core:krosstalk-core",
    "core:krosstalk-client", "core:krosstalk-server",
    "plugins:krosstalk-compiler-plugin", "plugins:krosstalk-gradle-plugin",
    "ktor:krosstalk-ktor-server", "ktor:krosstalk-ktor-client"
)

