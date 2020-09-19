plugins {
    kotlin("multiplatform")

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
kotlin {
    jvm {
        compilations.all {
            kotlinOptions.apply {
                jvmTarget = "1.8"
            }
        }
    }
    js(BOTH) {
        browser { }
        configure(compilations) {
            kotlinOptions {
                noStdlib = true
                sourceMapEmbedSources = "always"
                metaInfo = true
                sourceMap = true
                moduleKind = "commonjs"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":krosstalk"))
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-auth:$ktor_version")
            }
        }

        val jsMain by getting {
            dependencies {
                api(project(":krosstalk"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(project(":krosstalk"))
            }
        }

        all {
            languageSettings.apply {
                enableLanguageFeature("InlineClasses")
                enableLanguageFeature("NewInference")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
                useExperimentalAnnotation("kotlin.RequiresOptIn")
                useExperimentalAnnotation("kotlin.time.ExperimentalTime")
            }
        }
    }
}