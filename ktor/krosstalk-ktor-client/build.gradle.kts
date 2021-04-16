plugins {
    kotlin("multiplatform")
}

val ktor_version = "1.5.2"

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
                api("io.ktor:ktor-client-core:$ktor_version")
                api("io.ktor:ktor-client-auth:$ktor_version")
                api(project(":core:krosstalk-client"))
            }
        }

        val jsMain by getting {
            dependencies {
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
    }
}