plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
}

niceModuleName = "Krosstalk Kotlinx-serialization"
description = "Kotlinx-serialization support for Krosstalk"

kotlin {
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:krosstalk"))
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}