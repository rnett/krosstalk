import org.gradle.api.GradleException
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder

inline fun LanguageSettingsBuilder.commonSettings() {
    useExperimentalAnnotation("kotlin.RequiresOptIn")
}

inline fun KotlinMultiplatformExtension.allTargets() {
    jvm {
        //TODO remove once KT-36942 and KT-35003 are fixed
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
                //TODO remove once KT-36942 and KT-35003 are fixed
                compileJavaTaskProvider?.get()?.apply {
                    targetCompatibility = "1.8"
                    sourceCompatibility = "1.8"
                }
            }
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val isMacOs = hostOs == "Mac OS X"
    when {
        isMacOs -> {
            macosX64()
            ios()
            tvos()
            watchos()
        }
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        if (isMacOs) {
            val commonMain = getByName("commonMain")

            create("nativeMain") {
                dependsOn(commonMain)

                getByName("macosX64").dependsOn(this)
                getByName("ios").dependsOn(this)
                getByName("tvos").dependsOn(this)
                getByName("watchos").dependsOn(this)
            }
        }
    }

    sourceSets.all {
        languageSettings.commonSettings()
    }
}

inline fun KotlinJvmProjectExtension.irAndJava8() {
    target {
        //TODO remove once KT-36942 and KT-35003 are fixed
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                useIR = true
                //TODO remove once KT-36942 and KT-35003 are fixed
                compileJavaTaskProvider.get().apply {
                    targetCompatibility = "1.8"
                    sourceCompatibility = "1.8"
                }
            }
        }
        sourceSets.all {
            languageSettings.commonSettings()
        }
    }
}