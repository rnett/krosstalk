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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.useIR = true
    kotlinOptions.jvmTarget = "1.8"
}