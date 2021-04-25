plugins {
    kotlin("jvm") version "1.4.32"
}

version = "0.2.0-SNAPSHOT"

group = "com.github.rnett.krosstalk"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("io.ktor:ktor-client-core:1.5.3")
    implementation("io.ktor:ktor-client-cio:1.5.3")
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
            compileJavaTaskProvider.get().apply {
                targetCompatibility = "1.8"
                sourceCompatibility = "1.8"
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
}
