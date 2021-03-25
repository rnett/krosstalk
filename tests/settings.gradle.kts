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
rootProject.name = "tests"

includeBuild("..")
include("client-tests", "fullstack-tests")

