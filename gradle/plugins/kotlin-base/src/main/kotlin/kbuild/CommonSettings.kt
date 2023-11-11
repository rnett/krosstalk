package kbuild

import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder


fun LanguageSettingsBuilder.commonSettings() {
    optIn("kotlin.RequiresOptIn")
    optIn("kotlin.contracts.ExperimentalContracts")
}