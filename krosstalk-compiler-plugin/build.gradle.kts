plugins {
    kotlin("jvm")
    kotlin("kapt")
    `maven-publish` apply true
    id("com.github.johnrengelman.shadow") apply true
}

description = "Krosstalk Kotlin compiler plugin"

dependencies {
    implementation(kotlin("reflect"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.21")

    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc6")
    kapt("com.google.auto.service:auto-service:1.0-rc6")
}

kotlin {
    target.compilations.all {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    }
}

tasks.getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    configurations.add(project.configurations.compile.get())

    relocate("org.jetbrains.kotlin.com.intellij", "com.intellij")

    dependencies {
        exclude {
            it.moduleGroup == "org.jetbrains.kotlin"
        }
    }
}