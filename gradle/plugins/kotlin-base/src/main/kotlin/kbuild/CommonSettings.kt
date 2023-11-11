package kbuild

import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder


inline fun LanguageSettingsBuilder.commonSettings() {
    optIn("kotlin.RequiresOptIn")
    optIn("kotlin.contracts.ExperimentalContracts")
}