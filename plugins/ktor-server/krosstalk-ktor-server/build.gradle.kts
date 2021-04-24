plugins {
    kotlin("jvm")
}

dependencies {
    api("io.ktor:ktor-server-core:${Dependencies.ktor}")
    api(project(":core:krosstalk-server"))
}

kotlin.irAndJava8()
