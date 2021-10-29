package com.rnett.krosstalk

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class KrosstalkGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        //TODO workaround for https://youtrack.jetbrains.com/issue/KT-49360
        kotlinCompilation.kotlinOptions.freeCompilerArgs += "-Xverify-compiler=false"
        val project = kotlinCompilation.target.project
        return project.provider { emptyList() }
    }

    override fun getCompilerPluginId(): String = "com.rnett.krosstalk-compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        BuildConfig.PROJECT_GROUP_ID,
        BuildConfig.PROJECT_ARTIFACT_ID,
        BuildConfig.PROJECT_VERSION
    )

    override fun getPluginArtifactForNative(): SubpluginArtifact = SubpluginArtifact(
        BuildConfig.PROJECT_GROUP_ID,
        BuildConfig.PROJECT_ARTIFACT_ID + "-native",
        BuildConfig.PROJECT_VERSION
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
}