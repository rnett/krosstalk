plugins {
    kotlin("jvm")
}

dependencies {
    api("io.ktor:ktor-auth:${Dependencies.ktor}")
    api(project(":plugins:ktor-server:krosstalk-ktor-server"))
}

kotlin.irAndJava8()
