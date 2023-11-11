package kbuild

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class MultiplatformExtension {

    abstract val ktorTargetsOnly: Property<Boolean>

    abstract val publicationsFromMainHost: SetProperty<String>

    companion object {
        const val NAME = "kbuildMultiplatform"
    }
}