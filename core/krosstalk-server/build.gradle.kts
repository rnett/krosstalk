plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

description = "Krosstalk runtime library"

kotlin {
    allTargets(project)
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:krosstalk"))
            }
        }
    }
}