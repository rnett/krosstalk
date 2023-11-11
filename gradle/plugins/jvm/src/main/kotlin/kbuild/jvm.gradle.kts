package kbuild

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    target {
        //TODO remove once KT-36942 and KT-35003 are fixed
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "1.8"
                //TODO remove once KT-36942 and KT-35003 are fixed
                compileJavaTaskProvider?.get()?.apply {
                    targetCompatibility = "1.8"
                    sourceCompatibility = "1.8"
                }
            }
        }
    }
    sourceSets.configureEach {
        languageSettings {
            commonSettings()
        }
    }
}