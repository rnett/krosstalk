plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

description = "Auth scopes for the Krosstalk Ktor server plugin"

dependencies {
    api(libs.ktor.server.auth)
    api(project(":plugins:ktor-server:krosstalk-ktor-server"))
}

kotlin.irAndJava8()
