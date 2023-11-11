package kbuild

plugins {
    kotlin("jvm")
    id("kbuild.kotlin-base")
}

val libs = versionCatalogs.named("libs")

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.findLibrary("kotlinx.coroutines.test").orElseThrow())
    testImplementation(libs.findLibrary("mockk").orElseThrow())
}

tasks.withType<Test> {
    useJUnitPlatform()
}