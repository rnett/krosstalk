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
rootProject.name = "krosstalk"

include(
        "krosstalk-compiler-plugin", "krosstalk-gradle-plugin", "krosstalk",
        "krosstalk-ktor-server", "krosstalk-ktor-client",
        "fullstack-sample", "client-sample"
)

