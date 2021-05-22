plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.rnett.compiler-plugin-utils") version Dependencies.compilerPluginUtils
}

description = "Krosstalk Kotlin compiler plugin"

dependencies {
    implementation(kotlin("reflect"))
    implementation(project(":core:krosstalk-base"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Dependencies.kotlin}")

    implementation("com.github.rnett.compiler-plugin-utils:compiler-plugin-utils:${Dependencies.compilerPluginUtils}")

    compileOnly("com.google.auto.service:auto-service-annotations:${Dependencies.autoService}")
    kapt("com.google.auto.service:auto-service:${Dependencies.autoService}")
}

kotlin.irAndJava8(this)