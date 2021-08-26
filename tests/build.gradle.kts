plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
//    id("org.jetbrains.compose") version "0.4.0-build211" apply false
}


allprojects {
    group = "krosstalk.tests"
    version = "1.0.3-ALPHA"

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots"){
            mavenContent{ snapshotsOnly() }
        }
        maven("https://dl.bintray.com/kotlin/ktor")
    }
}
