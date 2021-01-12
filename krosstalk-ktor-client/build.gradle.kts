plugins {
    kotlin("multiplatform")
}

val ktor_version = "1.5.0"

description = "Basic Ktor server support for Krosstalk"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.apply {
                jvmTarget = "1.8"
                useIR = true
            }
        }
    }
    js(IR) {
//        moduleName = project.name
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":krosstalk"))
                api("io.ktor:ktor-client-core:$ktor_version")
                api("io.ktor:ktor-client-auth:$ktor_version")
            }
        }

        val jsMain by getting {
            dependencies {
                api(project(":krosstalk"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(project(":krosstalk"))
            }
        }

        all {
            languageSettings.apply {
                enableLanguageFeature("InlineClasses")
                enableLanguageFeature("NewInference")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
                useExperimentalAnnotation("kotlin.RequiresOptIn")
                useExperimentalAnnotation("kotlin.time.ExperimentalTime")
            }
        }
    }
}