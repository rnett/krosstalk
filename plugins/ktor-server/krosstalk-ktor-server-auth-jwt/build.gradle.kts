plugins {
    kotlin("jvm")
}

niceModuleName = "Krosstalk Ktor Server JWT Auth"
description = "JWT Auth scope for the Krosstalk Ktor server plugin"

dependencies {
    api("io.ktor:ktor-auth-jwt:${Dependencies.ktor}")
    api(project(":plugins:ktor-server:krosstalk-ktor-server-auth"))
}

kotlin.irAndJava8(project)
