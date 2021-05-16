plugins {
    kotlin("multiplatform")
}

description = "Kotlinx-serialization support for Krosstalk"

kotlin {
    allTargets(project)
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:krosstalk"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:${Dependencies.serialization}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${Dependencies.serialization}")
            }
        }
    }
}