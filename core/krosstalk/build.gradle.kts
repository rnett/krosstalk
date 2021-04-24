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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Dependencies.serialization}")
            }
        }

        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }
    }
}