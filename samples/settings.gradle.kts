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
rootProject.name = "samples"

includeBuild("..")
include("client-sample", "fullstack-sample")

