plugins {
    `java-gradle-plugin` apply true
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
    id("com.gradle.plugin-publish")
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.4.0")

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0")

    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc6")
    kapt("com.google.auto.service:auto-service:1.0-rc6")
}

buildConfig {
    val project = project(":krosstalk-compiler-plugin")
    packageName(project.group.toString())
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

pluginBundle {
    website = "https://github.com/rnett/krosstalk"
    vcsUrl = "https://github.com/rnett/krosstalk.git"
    tags = listOf("kotlin", "js", "server", "api")
}

tasks.getByName("compileKotlin") {
    dependsOn(":krosstalk-compiler-plugin:publishToMavenLocal")
}