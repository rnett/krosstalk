plugins {
    kotlin("jvm")
    `maven-publish` apply true
}

val ktor_version = "1.4.1"

description = "Basic Ktor server support for Krosstalk"

dependencies {
    api("io.ktor:ktor-server-core:$ktor_version")
    api("io.ktor:ktor-auth:$ktor_version")
    api(project(":krosstalk"))
}

kotlin {

    target.compilations.all {
        kotlinOptions {
            jvmTarget = "1.8"
            useIR = true
        }
    }
}
