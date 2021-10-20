plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
}

description = "Ktor client plugin for Krosstalk"

kotlin {
    allTargets(true)
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.ktor.client.core)
                api(project(":core:krosstalk-client"))
            }
        }
    }
}