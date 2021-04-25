plugins {
    kotlin("multiplatform") version "1.4.32" apply false
    kotlin("jvm") version "1.4.32" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.32" apply false
    id("org.jetbrains.compose") version "0.4.0-build183" apply false
    id("com.rnett.krosstalk")
    id("com.github.hesch.execfork") version "0.1.15"
}

allprojects {
    group = "krosstalk.tests"
    version = "1.0.3-ALPHA"

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
