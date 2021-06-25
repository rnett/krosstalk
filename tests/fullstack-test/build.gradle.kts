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
                implementation("com.github.rnett.krosstalk:krosstalk")
                implementation("com.github.rnett.krosstalk:krosstalk-kotlinx-serialization")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-cio:$ktor_version")

                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server-auth")

                implementation("ch.qos.logback:logback-classic:1.2.3")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client-auth")
                implementation("io.ktor:ktor-client-logging:$ktor_version")
            }
        }
    }
}

tasks.create<com.github.psxpaul.task.JavaExecFork>("startTestServer") {
    group = "verification"

    afterEvaluate {
        classpath = configurations["jvmRuntimeClasspath"] + kotlin.targets["jvm"].compilations["main"].output.allOutputs
    }

    main = "com.rnett.krosstalk.fullstack_test.TestKt"
    doLast {
        Thread.sleep(5_000)
    }

    dependsOn("jvmJar")

    stopAfter = tasks["jsBrowserTest"]

    tasks["jsBrowserTest"].dependsOn(this)
}
