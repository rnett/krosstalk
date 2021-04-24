plugins {
    kotlin("multiplatform")
}

description = "Basic Ktor server support for Krosstalk"

kotlin {
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.ktor:ktor-client-auth:${Dependencies.ktor}")
                api(project(":plugins:ktor-client:krosstalk-ktor-client"))
            }
        }
    }
}