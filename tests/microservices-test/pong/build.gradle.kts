import com.rnett.krosstalk.krosstalkClient
import com.rnett.krosstalk.krosstalkServer

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.rnett.krosstalk")
    id("com.github.psxpaul.execfork")
}

var ktor_version: String by extra
var coroutines_version: String by extra
val serialization_version: String by extra

kotlin {
    jvm("server") {
//        withJava()
        krosstalkServer()
    }
    jvm("client") {
        krosstalkClient()
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
                implementation(kotlin("test"))
            }
        }
        val serverMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-cio:$ktor_version")

                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server-auth")

                implementation("ch.qos.logback:logback-classic:1.2.3")

                implementation(project(":microservices-test:ping").krosstalkClient())
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
    }
}

tasks.create<com.github.psxpaul.task.JavaExecFork>("startAllTestsServer") {
    group = "verification"

    afterEvaluate {
        classpath = configurations["serverRuntimeClasspath"] + kotlin.targets["server"].compilations["main"].output.allOutputs
    }

    main = "com.rnett.krosstalk.pong.Main"

    dependsOn("serverJar")
}


val startLocalTestServer = tasks.create<com.github.psxpaul.task.JavaExecFork>("startLocalTestServer") {
    group = "verification"

    afterEvaluate {
        classpath = configurations["serverRuntimeClasspath"] + kotlin.targets["server"].compilations["main"].output.allOutputs
    }

    main = "com.rnett.krosstalk.pong.TestServer"

    dependsOn("serverJar")

    stopAfter = tasks["clientTest"]

    tasks["clientTest"].dependsOn(this)
}

tasks.getByName("clientTest") {
    doLast {
        startLocalTestServer.stop()
    }
}
