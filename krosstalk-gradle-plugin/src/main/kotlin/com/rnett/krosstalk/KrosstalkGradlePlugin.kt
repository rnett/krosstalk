package com.rnett.krosstalk

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class KrosstalkGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        return project.provider { emptyList() }
    }

    override fun getCompilerPluginId(): String = "com.rnett.krosstalk-compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        "com.rnett.krosstalk",
        "krosstalk-compiler-plugin",
        "1.0-SNAPSHOT"
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
}