plugins {
    kotlin("jvm")
}

description = "Ktor server plugin for Krosstalk"

dependencies {
    api("io.ktor:ktor-server-core:${Dependencies.ktor}")
    api(project(":core:krosstalk-server"))
}

kotlin.irAndJava8(project)
