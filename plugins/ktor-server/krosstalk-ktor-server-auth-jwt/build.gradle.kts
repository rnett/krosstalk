plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

niceModuleName = "Krosstalk Ktor Server JWT Auth"
description = "JWT Auth scope for the Krosstalk Ktor server plugin"

dependencies {
    api(libs.ktor.server.auth.jwt)
    api(project(":plugins:ktor-server:krosstalk-ktor-server-auth"))
}

kotlin.irAndJava8()
