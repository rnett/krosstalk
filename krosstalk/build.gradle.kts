plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")

}

val serialization_version = "1.0.0-RC2"

description = "Krosstalk runtime library"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
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
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
            }
        }

        val jsMain by getting {

        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
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