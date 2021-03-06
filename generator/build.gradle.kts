plugins {
    kotlin("jvm") version "1.5.20"
}

version = "0.2.0-SNAPSHOT"

group = "com.github.rnett.krosstalk"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots"){
        mavenContent{ snapshotsOnly() }
    }
}

dependencies {
    implementation("io.ktor:ktor-client-core:1.6.0")
    implementation("io.ktor:ktor-client-cio:1.6.0")
    implementation("org.jsoup:jsoup:1.13.1")
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                useIR = true
                jvmTarget = "1.8"
            }
            //TODO remove once KT-36942 and KT-35003 are fixed
            compileJavaTaskProvider.configure {
                targetCompatibility = "1.8"
                sourceCompatibility = "1.8"
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
}
