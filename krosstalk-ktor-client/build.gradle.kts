plugins {
    kotlin("multiplatform")
}

val ktor_version = "1.4.0"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.apply {
                jvmTarget = "1.8"
            }
        }
    }
    js(IR) {
        browser { }
    }

    configure(listOf(targets["metadata"], jvm(), js())) {
        mavenPublication {
            val targetPublication = this@mavenPublication
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":krosstalk"))
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-auth:$ktor_version")
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