import com.rnett.krosstalk.krosstalkClient
import com.rnett.krosstalk.krosstalkServer

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
    id("com.rnett.krosstalk")
    id("com.github.hesch.execfork")
}

val ktor_version = "1.5.2"
val coroutines_version = "1.4.3"
val serialization_version = "1.1.0"

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

kotlin {
    jvm("server") {
        withJava()
        krosstalkServer()
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
            }
        }
    }
    jvm("client") {
        krosstalkClient()
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
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

                implementation("com.github.rnett.krosstalk:krosstalk-client")

                implementation("ch.qos.logback:logback-classic:1.2.3")

                implementation(project(":microservices-test:pong").krosstalkClient())
            }
        }
        val serverTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val clientMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client-auth")
                implementation("io.ktor:ktor-client-apache:$ktor_version")
                implementation("io.ktor:ktor-client-logging:$ktor_version")
            }
        }
        val clientTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
application {
    this.mainClass.set("com.rnett.krosstalk.ping.Main")
}

tasks.create<com.github.psxpaul.task.JavaExecFork>("startAllTestsServer") {
    group = "verification"

    classpath = sourceSets.main.get().runtimeClasspath
    main = "com.rnett.krosstalk.ping.Main"

    dependsOn("serverJar")
}

val startLocalTestServer = tasks.create<com.github.psxpaul.task.JavaExecFork>("startLocalTestServer") {
    group = "verification"

    classpath = sourceSets.main.get().runtimeClasspath
    main = "com.rnett.krosstalk.ping.TestServer"

    dependsOn("serverJar")

    stopAfter = tasks["clientTest"]

    tasks["clientTest"].dependsOn(this)
}

tasks.getByName("clientTest") {
    doLast {
        startLocalTestServer.stop()
    }
}
