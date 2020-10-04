plugins {
    kotlin("jvm")
    `maven-publish`
}

val ktor_version = "1.4.1"

description = "Basic Ktor server support for Krosstalk"

dependencies {
    api("io.ktor:ktor-server-core:$ktor_version")
    api("io.ktor:ktor-auth:$ktor_version")
    api(project(":krosstalk"))
}

kotlin {
    sourceSets.all {
        languageSettings.apply {
            enableLanguageFeature("InlineClasses")
            enableLanguageFeature("NewInference")
            useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            useExperimentalAnnotation("kotlin.RequiresOptIn")
            useExperimentalAnnotation("kotlin.time.ExperimentalTime")
        }
    }

    target.compilations.all {
        kotlinOptions {
            jvmTarget = "1.8"
            useIR = true
        }
    }
}
