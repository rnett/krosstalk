plugins {
    kotlin("multiplatform") version "1.4.0" apply false
    kotlin("jvm") version "1.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.0" apply false
    kotlin("kapt") version "1.4.0" apply false
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
}

group = "com.rnett.krosstalk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    google()
    mavenLocal()
    maven {
        url = uri("https://dl.bintray.com/kotlin/ktor")
    }
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        google()
        mavenLocal()
        maven {
            url = uri("https://dl.bintray.com/kotlin/ktor")
        }
        maven {
            url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.useIR = true
    }
}