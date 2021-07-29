plugins {
    kotlin("multiplatform") version "1.5.21" apply false
    kotlin("jvm") version "1.5.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.21" apply false
//    id("org.jetbrains.compose") version "0.4.0-build211" apply false
    id("com.github.rnett.krosstalk") apply false
    id("com.github.psxpaul.execfork") version "0.1.15" apply false
}


allprojects {
    group = "krosstalk.tests"
    version = "1.0.3-ALPHA"

    var ktor_version: String by extra("1.6.1")
    var coroutines_version: String by extra("1.5.1")
    val serialization_version: String by extra("1.2.2")

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots"){
            mavenContent{ snapshotsOnly() }
        }
        maven("https://dl.bintray.com/kotlin/ktor")
    }
}
