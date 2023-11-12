package kbuild

import org.gradle.api.provider.SetProperty

abstract class MultiplatformExtension {

    abstract val onlyPublishFromMainHost: SetProperty<String>

    companion object {
        const val NAME = "kbuildMultiplatform"
        val isMainHost get() = OS.isWindows
    }
}