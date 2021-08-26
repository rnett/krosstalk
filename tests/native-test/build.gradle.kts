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

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk")
                implementation("com.github.rnett.krosstalk:krosstalk-kotlinx-serialization")
                implementation(libs.kotlinx.serialization.cbor)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}-native-mt")
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

                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-server-auth")

                implementation(libs.logback)
            }
        }
        val nativeMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk-client")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client-auth")

                implementation(libs.ktor.client.curl)
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

    main = "com.rnett.krosstalk.native_test.TestKt"
    doLast {
        Thread.sleep(5_000)
    }

    dependsOn("jvmJar")

    stopAfter = tasks["nativeTest"]

    tasks["nativeTest"].dependsOn(this)
}
