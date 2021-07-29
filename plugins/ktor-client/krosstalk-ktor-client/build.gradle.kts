plugins {
    kotlin("multiplatform")
}

description = "Ktor client plugin for Krosstalk"

kotlin {
    allTargets(false)
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.ktor:ktor-client-core:${Dependencies.ktor}")
                api(project(":core:krosstalk-client"))
            }
        }
    }
}