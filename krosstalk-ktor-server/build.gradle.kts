plugins {
    kotlin("jvm") apply true

}
group = "com.rnett.krosstalk"
version = "1.0-SNAPSHOT"

val ktor_version = "1.4.0"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kotlin/ktor")
    }
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}
dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
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
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    }
}