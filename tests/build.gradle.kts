plugins {
    kotlin("multiplatform") version "1.5.10" apply false
    kotlin("jvm") version "1.5.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.0" apply false
    id("org.jetbrains.compose") version "0.4.0-build209" apply false
    id("com.github.rnett.krosstalk") apply false
    id("com.github.hesch.execfork") version "0.1.15" apply false
}


allprojects {
    group = "krosstalk.tests"
    version = "1.0.3-ALPHA"

    var ktor_version: String by extra("2.0.0-eap-132")
    var coroutines_version: String by extra("1.5.0")
    val serialization_version: String by extra("1.2.1")

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
}
