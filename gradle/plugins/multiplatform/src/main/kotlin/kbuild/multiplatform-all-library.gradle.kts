package kbuild

import kbuild.OS.ifLinux
import kbuild.OS.ifMac
import kbuild.OS.ifWindows

plugins {
    id("kbuild.multiplatform-library")
}

kotlin {
    jvm {

    }

    js {
        browser()
        nodejs()
    }

    ifWindows {
        mingwX64()
    }

    ifMac {
        macosX64()
        macosArm64()

        iosSimulatorArm64()
        iosX64()
        iosArm64()

        watchosSimulatorArm64()
        watchosX64()
        watchosArm32()
        watchosArm64()
        tvosSimulatorArm64()
        tvosX64()
        tvosArm64()

    }

    ifLinux {
        linuxX64()
        linuxArm64()
    }
}