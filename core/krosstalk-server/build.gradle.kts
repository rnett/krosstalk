plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlinx.serialization)
}

description = "The server-specific APIs of Krosstalk, including KrosstalkServer."

kotlin {
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:krosstalk"))
            }
        }
    }
}