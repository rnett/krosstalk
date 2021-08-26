plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlinx.serialization)
}

description = "The client-specific APIs of Krosstalk, including KrosstalkClient."

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