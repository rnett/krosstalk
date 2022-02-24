import com.rnett.krosstalk.krosstalkClient
import com.rnett.krosstalk.krosstalkServer

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlinx.serialization)
    id("com.github.rnett.krosstalk")
    id(libs.plugins.execfork.get().pluginId)
}

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
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val serverMain by getting {
            dependencies {
                implementation(libs.ktor.server.cio)

                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server-auth")

                implementation("com.github.rnett.krosstalk:krosstalk-client")

                implementation(libs.logback)

                implementation(project(":microservices-test:pong").krosstalkClient())
            }
        }
        val clientMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client-auth")
                implementation(libs.ktor.client.apache)
                implementation(libs.ktor.client.logging)
            }
        }
    }
}

tasks.create<com.github.psxpaul.task.JavaExecFork>("startAllTestsServer") {
    group = "verification"

    afterEvaluate {
        classpath = configurations["serverRuntimeClasspath"] + kotlin.targets["server"].compilations["main"].output.allOutputs
    }

    main = "com.rnett.krosstalk.ping.Main"

    dependsOn("serverJar")
}

val startLocalTestServer = tasks.create<com.github.psxpaul.task.JavaExecFork>("startLocalTestServer") {
    group = "verification"

    afterEvaluate {
        classpath = configurations["serverRuntimeClasspath"] + kotlin.targets["server"].compilations["main"].output.allOutputs
    }

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
