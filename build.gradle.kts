plugins {
    kotlin("multiplatform") version "1.4.32" apply false
    kotlin("jvm") version "1.4.32" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.32" apply false
    kotlin("kapt") version "1.4.32" apply false
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
//    id("com.jfrog.bintray") version "1.8.5" apply false
    id("com.gradle.plugin-publish") version "0.11.0" apply false
    id("org.jetbrains.dokka") version "1.4.20" apply false
    id("com.github.gmazzo.buildconfig") version "2.0.2" apply false
}

allprojects {
    version = "0.2.0-SNAPSHOT"

    group = "com.github.rnett.krosstalk"

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.useIR = true
        kotlinOptions.jvmTarget = "1.8"
    }
}