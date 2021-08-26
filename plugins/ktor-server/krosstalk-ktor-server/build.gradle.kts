plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

description = "Ktor server plugin for Krosstalk"

dependencies {
    api(libs.ktor.server.core)
    api(project(":core:krosstalk-server"))
}

kotlin.irAndJava8()
