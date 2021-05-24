plugins {
    kotlin("multiplatform")
}

niceModuleName = "Krosstalk Kotlinx-serialization"
description = "Kotlinx-serialization support for Krosstalk"

kotlin {
    allTargets()
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