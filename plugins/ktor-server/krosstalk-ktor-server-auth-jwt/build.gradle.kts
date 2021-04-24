plugins {
    kotlin("jvm")
}

dependencies {
    api("io.ktor:ktor-auth-jwt:${Dependencies.ktor}")
    api(project(":plugins:ktor-server:krosstalk-ktor-server-auth"))
}

kotlin.irAndJava8()
