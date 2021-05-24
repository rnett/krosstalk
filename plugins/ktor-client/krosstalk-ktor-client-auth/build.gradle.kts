plugins {
    kotlin("multiplatform")
}

description = "Auth scopes for the Krosstalk Ktor client plugin"

kotlin {
    allTargets(true)
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.ktor:ktor-client-auth:${Dependencies.ktor}")
                api(project(":plugins:ktor-client:krosstalk-ktor-client"))
            }
        }
    }
}