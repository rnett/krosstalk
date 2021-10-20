plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlinx.serialization)
    id("com.github.rnett.krosstalk")
    alias(libs.plugins.execfork)
}

kotlin {
    jvm {
//        withJava()
    }
    js(IR) {
        browser {
            binaries.executable()
            testTask {
                useMocha {
                    timeout = "5m"
                }

            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.serialization)
                implementation(libs.ktor.server.auth)
                implementation(libs.ktor.server.cors)
                implementation(libs.ktor.server.content.negotiation)

                implementation(libs.logback)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk")
                implementation("com.github.rnett.krosstalk:krosstalk-kotlinx-serialization")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client-auth")

                implementation(libs.ktor.client.logging)
            }
        }
    }
}

tasks.create<com.github.psxpaul.task.JavaExecFork>("startTestServer") {
    group = "verification"
    afterEvaluate {
        classpath = configurations["jvmRuntimeClasspath"] + kotlin.targets["jvm"].compilations["main"].output.allOutputs
    }
    main = "com.rnett.krosstalk.client_test.TestKt"
    doLast {
        Thread.sleep(5_000)
    }

    dependsOn("jvmJar")

    stopAfter = tasks["jsBrowserTest"]

    tasks["jsBrowserTest"].dependsOn(this)
}