plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}


description = "Krosstalk runtime library"

kotlin {
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:krosstalk-core"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:${Dependencies.serialization}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-cbor:${Dependencies.serialization}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${Dependencies.serialization}")
            }
        }

        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }
    }
}