plugins {
    kotlin("jvm")
    `maven-publish` apply true
}

description = "Basic Ktor server support for Krosstalk"

dependencies {
    api("io.ktor:ktor-server-core:${Dependencies.ktor}")
    api("io.ktor:ktor-auth:${Dependencies.ktor}")
    api(project(":core:krosstalk-server"))
}

kotlin.irAndJava8()

tasks.compileKotlin.configure {
    this.javaPackagePrefix
}
