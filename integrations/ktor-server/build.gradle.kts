import kbuild.OS

plugins {
    id("kbuild.multiplatform-library")
}

kotlin {
    // Targets from https://ktor.io/docs/supported-platforms.html

    jvm()

    OS.ifMac {
        iosArm64()
        iosX64()
        iosSimulatorArm64()
        watchosArm32()
        watchosArm64()
        watchosX64()
        watchosSimulatorArm64()
        tvosArm64()
        tvosX64()
        tvosSimulatorArm64()
        macosX64()
        macosArm64()
    }

    OS.ifLinux {
        linuxX64()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":core"))

                api(libs.ktor.server.core)
            }
        }
    }
}