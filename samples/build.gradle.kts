//import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
//import org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact

plugins {
    kotlin("multiplatform") version "1.4.31" apply false
    kotlin("jvm") version "1.4.31" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.31" apply false
    id("com.rnett.krosstalk")
    id("com.github.hesch.execfork") version "0.1.15"
}

allprojects {
    group = "com.rnett.krosstalk"
    version = "1.0.3-ALPHA"

    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        mavenCentral()
        maven("https://oss.jfrog.org/artifactory/oss-snapshot-local/")
        google()
        jcenter()
        mavenLocal()
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }
    }
}
