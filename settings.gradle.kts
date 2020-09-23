pluginManagement {
    repositories {
        jcenter()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
        }
        mavenLocal()
    }

}
enableFeaturePreview("GRADLE_METADATA")
rootProject.name = "krosstalk"

include(
    "krosstalk-compiler-plugin", "krosstalk-gradle-plugin", "krosstalk",
    "krosstalk-ktor-server",
    "krosstalk-ktor-client",
    "sample"
)

