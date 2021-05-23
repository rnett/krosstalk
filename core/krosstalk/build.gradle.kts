plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

niceModuleName = "Krosstalk Core"
description = "The core (client or server) APIs of Krosstalk, including everything necessary for expect Krosstalks."

kotlin {
    allTargets(project)
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:krosstalk-base"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Dependencies.serialization}")
            }
        }

        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }
    }
}