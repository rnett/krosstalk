plugins {
    kotlin("multiplatform")
}

description = "Basic Ktor server support for Krosstalk"

kotlin {
    allTargets(project, true)
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.ktor:ktor-client-core:${Dependencies.ktor}")
                api(project(":core:krosstalk-client"))
            }
        }
    }
}