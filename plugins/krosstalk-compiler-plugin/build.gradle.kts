plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.rnett.compiler-plugin-utils") version "0.1.1-SNAPSHOT"
    `maven-publish` apply true
}

description = "Krosstalk Kotlin compiler plugin"

dependencies {
    implementation(kotlin("reflect"))
    implementation(project(":core:krosstalk-core"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.31")

    implementation("com.github.rnett.compiler-plugin-utils:compiler-plugin-utils:0.1.1-SNAPSHOT")

    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc6")
    kapt("com.google.auto.service:auto-service:1.0-rc6")
}

kotlin {
    target.compilations.all {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    }
}