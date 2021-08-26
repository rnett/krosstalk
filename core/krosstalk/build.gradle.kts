plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlinx.serialization)
}

niceModuleName = "Krosstalk Core"
description = "The core (client or server) APIs of Krosstalk, including everything necessary for expect Krosstalks."

kotlin {
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:krosstalk-base"))
                implementation(libs.kotlinx.serialization.json)
            }
        }

        all {
            languageSettings.apply {
                optIn("kotlin.contracts.ExperimentalContracts")
            }
        }
    }
}