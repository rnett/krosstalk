plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
}

description = "Auth scopes for the Krosstalk Ktor client plugin"

kotlin {
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.ktor.client.auth)
                api(project(":plugins:ktor-client:krosstalk-ktor-client"))
            }
        }
    }
}