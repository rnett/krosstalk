plugins {
    kotlin("jvm")
}

description = "Auth scopes for the Krosstalk Ktor server plugin"

dependencies {
    api("io.ktor:ktor-auth:${Dependencies.ktor}")
    api(project(":plugins:ktor-server:krosstalk-ktor-server"))
}

kotlin.irAndJava8()
