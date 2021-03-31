pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }
    }

}
//enableFeaturePreview("GRADLE_METADATA")
rootProject.name = "samples"

includeBuild("..")
include("client-sample", "fullstack-sample")

