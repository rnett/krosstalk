plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
    id("com.github.rnett.krosstalk")
    id("com.github.hesch.execfork")
}

var ktor_version: String by extra
var coroutines_version: String by extra
val serialization_version: String by extra

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/ktor")
}
kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        withJava()
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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version-native-mt")
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
        val nativeMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk-client")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client")
                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client-auth")

                implementation("io.ktor:ktor-client-curl:$ktor_version")
                implementation("io.ktor:ktor-client-logging:$ktor_version")
            }
        }
    }
}

application {
    this.mainClass.set("com.rnett.krosstalk.native_test.TestKt")
}

tasks.create<com.github.psxpaul.task.JavaExecFork>("startTestServer") {
    group = "verification"

    classpath = sourceSets.main.get().runtimeClasspath
    main = "com.rnett.krosstalk.native_test.TestKt"
    doLast {
        Thread.sleep(5_000)
    }

    dependsOn("jvmJar")

    stopAfter = tasks["nativeTest"]

    tasks["nativeTest"].dependsOn(this)
}
