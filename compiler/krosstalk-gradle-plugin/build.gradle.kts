plugins {
    `java-gradle-plugin` apply true
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.kapt.get().pluginId)
    alias(libs.plugins.buildconfig)
//    id("com.gradle.plugin-publish")
    //TODO publish to portal, see https://github.com/vanniktech/gradle-maven-publish-plugin/issues/256
}

description = "Krosstalk gradle plugin, for serving the compiler plugin"

dependencies {
    implementation(libs.kgp.api)

    compileOnly(libs.kgp)

    compileOnly(libs.autoservice.annotations)
    kapt(libs.autoservice)
}

kotlin.irAndJava8()

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