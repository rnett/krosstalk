package kbuild

plugins {
    kotlin("multiplatform")
}

val hostOs: String get() = System.getProperty("os.name")
val isMingwX64 get() = hostOs.startsWith("Windows")
val isMacOs get() = hostOs == "Mac OS X"

val isMainHost get() = isMingwX64

val extension = extensions.create<MultiplatformExtension>(MultiplatformExtension.NAME).apply {
    ktorTargetsOnly.convention(false)
    publicationsFromMainHost.convention(setOf("jvm", "js", "kotlinMultiplatform"))
}

kotlin {
    jvmToolchain(21)
    jvm {
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

    js {
        browser()
        nodejs()
    }

    val nativeTargets = when {
        isMacOs -> listOf(
            macosX64(),

            iosArm64(),
            iosX64(),

            tvosArm64(),
            tvosX64(),

            watchosArm32(),
            watchosArm64(),
            watchosX64(),
        ) + if (!extension.ktorTargetsOnly.get()) listOf(
            macosArm64(),

            iosSimulatorArm64(),
            tvosSimulatorArm64(),
            watchosSimulatorArm64(),
        ) else emptyList()

        hostOs == "Linux" -> listOf(
            linuxX64(),
        ) + if (!extension.ktorTargetsOnly.get()) listOf(
            linuxArm64(),
        ) else emptyList()

        isMingwX64 -> listOf(
            mingwX64(),
        )

        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets.configureEach {
        languageSettings.commonSettings()
    }
}



extensions.findByType<PublishingExtension>()?.let {
    extensions.configure<PublishingExtension> {
        publications {
            configureEach {
                if (name in extension.publicationsFromMainHost.get()) {
                    tasks.withType<AbstractPublishToMaven>()
                        .matching { it.publication == this }
                        .configureEach {
                            onlyIf {
                                isMainHost
                            }
                        }
                }
            }
        }
    }
}