import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
    id("com.rnett.krosstalk") version "1.0.1-ALPHA"
}

val ktor_version = "1.4.1"
val coroutines_version = "1.3.9"
val serialization_version = "1.0.0-RC2"

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}
kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
            }
        }
        withJava() //TODO report weird errors when missing this
    }
    js(IR) {
//        produceExecutable()

        browser {
            binaries.executable()

            webpackTask {
                val project = project
                mode = if (project.hasProperty("dev")) Mode.DEVELOPMENT else Mode.PRODUCTION
            }
        }


//        configure(compilations) {
//            kotlinOptions {
////                noStdlib = true
//                sourceMapEmbedSources = "always"
//                metaInfo = true
//                sourceMap = true
////                moduleKind = "commonjs"
//            }
//        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-netty:$ktor_version")
                implementation("io.ktor:ktor-html-builder:$ktor_version")
                implementation("io.ktor:ktor-serialization:$ktor_version")

//                api("org.jetbrains.kotlinx:kotlinx-html:0.7.2")

                implementation("ch.qos.logback:logback-classic:1.2.3")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(project(":krosstalk-ktor-client"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
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

application {
    mainClassName = "com.rnett.krosstalk.client_sample.TestKt"
}
tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
    outputFileName = "test.js"
}
tasks.getByName<Jar>("jvmJar") {
    dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
    val jsBrowserProductionWebpack = tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack")
    from(File(jsBrowserProductionWebpack.destinationDirectory, jsBrowserProductionWebpack.outputFileName))
}
tasks.getByName<JavaExec>("run") {
    dependsOn(tasks.getByName<Jar>("jvmJar"))
    classpath(tasks.getByName<Jar>("jvmJar"))
}

tasks.getByName("compileKotlinJvm") {
    dependsOn(
        ":krosstalk-compiler-plugin:publishToMavenLocal",
        ":krosstalk-gradle-plugin:publishToMavenLocal"
    )
}

tasks.getByName("compileKotlinJs") {
    dependsOn(
        ":krosstalk-compiler-plugin:publishToMavenLocal",
        ":krosstalk-gradle-plugin:publishToMavenLocal"
    )
}