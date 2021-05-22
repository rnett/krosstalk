plugins {
    `java-gradle-plugin` apply true
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
    `maven-publish`
//    id("com.gradle.plugin-publish")
    //TODO publish to portal, see https://github.com/vanniktech/gradle-maven-publish-plugin/issues/256
}

description = "Krosstalk gradle plugin, for serving the compiler plugin"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${Dependencies.kotlin}")

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${Dependencies.kotlin}")

    compileOnly("com.google.auto.service:auto-service-annotations:${Dependencies.autoService}")
    kapt("com.google.auto.service:auto-service:${Dependencies.autoService}")
}

kotlin.irAndJava8(project)

buildConfig {
    val project = project(":compiler:krosstalk-compiler-plugin")
    packageName("com.rnett.krosstalk")
    buildConfigField("String", "PROJECT_GROUP_ID", "\"${project.group}\"")
    buildConfigField("String", "PROJECT_ARTIFACT_ID", "\"${project.name}\"")
    buildConfigField("String", "PROJECT_VERSION", "\"${project.version}\"")
}

gradlePlugin {
    plugins {
        create("krosstalkPlugin") {
            id = "com.github.rnett.krosstalk"
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

//pluginBundle {
//    website = "https://github.com/rnett/krosstalk"
//    vcsUrl = "https://github.com/rnett/krosstalk.git"
//    tags = listOf("kotlin", "js", "RPC", "server", "api")
//}