import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
    id("com.github.rnett.krosstalk")
    id("com.github.hesch.execfork")
    id("org.jetbrains.compose")
}

val ktor_version = "1.5.2"
val coroutines_version = "1.4.3"
val serialization_version = "1.1.0"

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}
kotlin {
    jvm("server") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    jvm("compose") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk")
                implementation("com.github.rnett.krosstalk:krosstalk-kotlinx-serialization")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")

                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val serverMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-cio:$ktor_version")

                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server-auth")

                implementation("ch.qos.logback:logback-classic:1.2.3")
                compileOnly(compose.desktop.currentOs)
            }
        }
        val serverTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val composeMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client-auth")
                implementation("io.ktor:ktor-client-apache:$ktor_version")
                implementation("io.ktor:ktor-client-logging:$ktor_version")
                implementation(compose.desktop.currentOs)
            }
        }
        val composeTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

application {
    this.mainClass.set("com.rnett.krosstalk.compose_test.TestKt")
}

//tasks.create<com.github.psxpaul.task.JavaExecFork>("startTestServer") {
//    group = "verification"
//
//    classpath = sourceSets.main.get().runtimeClasspath
//    main = "com.rnett.krosstalk.compose_test.TestKt"
//    Thread.sleep(2_000)
//
//    dependsOn("jvmJar")
//
//    stopAfter = tasks["jsTest"]
//
//    tasks["jsTest"].dependsOn(this)
//}
