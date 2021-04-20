plugins {
    kotlin("multiplatform")
}

description = "Common parts Krosstalk runtime library used by both the runtime and compiler plugin"

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