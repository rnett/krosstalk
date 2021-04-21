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
}
kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
            }
        }
        withJava()
    }
    js(IR) {
        browser {
            binaries.executable()

            webpackTask {
                val project = project
                mode =
                    if (project.hasProperty("dev")) org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.DEVELOPMENT else org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.PRODUCTION
            }
            testTask {
                useMocha {
                    timeout = "99999999999999"
//                    useChromeHeadless()
                }

            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
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
                implementation("io.ktor:ktor-server-cio:$ktor_version")
                implementation("io.ktor:ktor-serialization:$ktor_version")
                implementation("io.ktor:ktor-auth:$ktor_version")

                implementation("ch.qos.logback:logback-classic:1.2.3")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("com.github.rnett.krosstalk:krosstalk")

                implementation("com.github.rnett.krosstalk:krosstalk-ktor-client")

                api("io.ktor:ktor-client-core:$ktor_version")
                api("io.ktor:ktor-client-auth:$ktor_version")
                implementation("io.ktor:ktor-client-logging:$ktor_version")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

application {
    this.mainClass.set("com.rnett.krosstalk.client_test.TestKt")
}

tasks.create<com.github.psxpaul.task.JavaExecFork>("startTestServer") {
    group = "verification"

    classpath = sourceSets.main.get().runtimeClasspath
    main = "com.rnett.krosstalk.client_test.TestKt"
    Thread.sleep(2_000)

    dependsOn("jvmJar")

    stopAfter = tasks["jsBrowserTest"]

    tasks["jsBrowserTest"].dependsOn(this)
}