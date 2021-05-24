plugins {
    kotlin("multiplatform")
}

description = "The parts of Krosstalk that are used in both the runtime and compiler plugin."

kotlin {
    allTargets()
    sourceSets {
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
        }
    }
}