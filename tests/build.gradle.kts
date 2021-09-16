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
        afterEvaluate {
            if ("eap" in libs.versions.ktor.get().toLowerCase()) {
                maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
            }
        }
    }

    afterEvaluate {
        tasks.withType(AbstractTestTask::class) {
            this.testLogging {
                showStandardStreams = true
                showStackTraces = true
                showExceptions = true
                showCauses = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }
}



