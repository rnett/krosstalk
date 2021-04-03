plugins {
    `java-gradle-plugin` apply true
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
    id("com.gradle.plugin-publish")
    `maven-publish`
}

description = "Krosstalk gradle plugin, for serving the compiler plugin"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.4.32")

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")

    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc6")
    kapt("com.google.auto.service:auto-service:1.0-rc6")
}

buildConfig {
    val project = project(":plugins:krosstalk-compiler-plugin")
    packageName("com.rnett.krosstalk")
    buildConfigField("String", "PROJECT_GROUP_ID", "\"${project.group}\"")
    buildConfigField("String", "PROJECT_ARTIFACT_ID", "\"${project.name}\"")
    buildConfigField("String", "PROJECT_VERSION", "\"${project.version}\"")
}

gradlePlugin {
    plugins {
        create("krosstalkPlugin") {
            id = "com.rnett.krosstalk"
            displayName = "Krosstalk Plugin"
            description = "Krosstalk Kotlin Compiler plugin"
            implementationClass = "com.rnett.krosstalk.KrosstalkGradlePlugin"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

pluginBundle {
    website = "https://github.com/rnett/krosstalk"
    vcsUrl = "https://github.com/rnett/krosstalk.git"
    tags = listOf("kotlin", "js", "server", "api")
}