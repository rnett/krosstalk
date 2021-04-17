pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }
    }

}
//enableFeaturePreview("GRADLE_METADATA")
rootProject.name = "tests"

includeBuild("..")
include("client-test", "fullstack-test", "compose-test")

