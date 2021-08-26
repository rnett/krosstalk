plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.kapt.get().pluginId)
    alias(libs.plugins.compiler.plugin.utils)
}

description = "Krosstalk Kotlin compiler plugin"

dependencies {
    implementation(kotlin("reflect"))
    implementation(project(":core:krosstalk-base"))
    compileOnly(libs.kotlin.compiler.embeddable)

    implementation(libs.compiler.plugin.utils)

    compileOnly(libs.autoservice.annotations)
    kapt(libs.autoservice)
}

kotlin.irAndJava8()