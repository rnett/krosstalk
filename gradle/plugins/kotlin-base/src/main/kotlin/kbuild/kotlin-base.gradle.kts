package kbuild

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension


kotlinExtension.apply {
    jvmToolchain(21)

    sourceSets.configureEach {
        languageSettings {
            commonSettings()
        }
    }
}